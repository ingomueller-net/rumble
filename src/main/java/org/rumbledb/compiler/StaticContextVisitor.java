/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors: Stefan Irimescu, Can Berker Cikis
 *
 */

package org.rumbledb.compiler;

import java.util.ArrayList;
import java.util.List;

import org.rumbledb.context.Name;
import org.rumbledb.context.StaticContext;
import org.rumbledb.exceptions.OurBadException;
import org.rumbledb.exceptions.UndeclaredVariableException;
import org.rumbledb.exceptions.VariableAlreadyExistsException;
import org.rumbledb.expressions.AbstractNodeVisitor;
import org.rumbledb.expressions.Expression;
import org.rumbledb.expressions.Node;
import org.rumbledb.expressions.control.TypeSwitchExpression;
import org.rumbledb.expressions.control.TypeswitchCase;
import org.rumbledb.expressions.flowr.Clause;
import org.rumbledb.expressions.flowr.CountClause;
import org.rumbledb.expressions.flowr.FlworExpression;
import org.rumbledb.expressions.flowr.GroupByVariableDeclaration;
import org.rumbledb.expressions.flowr.ForClause;
import org.rumbledb.expressions.flowr.GroupByClause;
import org.rumbledb.expressions.flowr.LetClause;
import org.rumbledb.expressions.module.FunctionDeclaration;
import org.rumbledb.expressions.module.LibraryModule;
import org.rumbledb.expressions.module.VariableDeclaration;
import org.rumbledb.expressions.primary.FunctionCallExpression;
import org.rumbledb.expressions.primary.InlineFunctionExpression;
import org.rumbledb.expressions.primary.VariableReferenceExpression;
import org.rumbledb.expressions.quantifiers.QuantifiedExpression;
import org.rumbledb.expressions.quantifiers.QuantifiedExpressionVar;
import org.rumbledb.runtime.functions.base.BuiltinFunctionCatalogue;
import org.rumbledb.runtime.functions.base.FunctionIdentifier;
import org.rumbledb.runtime.functions.base.Functions;
import org.rumbledb.types.ItemType;
import org.rumbledb.types.SequenceType;
import org.rumbledb.types.SequenceType.Arity;

import sparksoniq.jsoniq.ExecutionMode;

/**
 * Static context visitor implements a multi-pass algorithm that enables function hoisting
 */
public class StaticContextVisitor extends AbstractNodeVisitor<StaticContext> {

    private VisitorConfig visitorConfig;

    StaticContextVisitor() {
        this.visitorConfig = VisitorConfig.staticContextVisitorInitialPassConfig;
    }

    void setVisitorConfig(VisitorConfig visitorConfig) {
        this.visitorConfig = visitorConfig;
    }

    @Override
    protected StaticContext defaultAction(Node node, StaticContext argument) {
        StaticContext generatedContext = visitDescendants(node, argument);
        // initialize execution mode by visiting children and expressions first, then calling initialize methods
        node.initHighestExecutionMode(this.visitorConfig);
        return generatedContext;
    }

    @Override
    public StaticContext visit(Node node, StaticContext argument) {
        if (argument == null) {
            throw new OurBadException("No static context provided!");
        }
        if (node instanceof Expression) {
            ((Expression) node).setStaticContext(argument);
        }
        return node.accept(this, argument);
    }

    // region primary
    @Override
    public StaticContext visitVariableReference(VariableReferenceExpression expression, StaticContext argument) {
        Name variableName = expression.getVariableName();
        if (!argument.isInScope(variableName)) {
            throw new UndeclaredVariableException(
                    "Uninitialized variable reference: " + variableName,
                    expression.getMetadata()
            );
        } else {
            expression.setType(argument.getVariableSequenceType(variableName));
            ExecutionMode mode = argument.getVariableStorageMode(variableName);
            if (this.visitorConfig.setUnsetToLocal() && mode.equals(ExecutionMode.UNSET)) {
                mode = ExecutionMode.LOCAL;
            }
            expression.setHighestExecutionMode(mode);
            return argument;
        }
    }

    private void populateFunctionDeclarationStaticContext(
            StaticContext functionDeclarationContext,
            List<ExecutionMode> modes,
            InlineFunctionExpression expression
    ) {
        int i = 0;
        for (Name name : expression.getParams().keySet()) {
            ExecutionMode mode = modes.get(i);
            SequenceType type = expression.getParams().get(name);
            if (type.isEmptySequence()) {
                mode = ExecutionMode.LOCAL;
            } else if (type.getArity().equals(Arity.OneOrZero) || type.getArity().equals(Arity.One)) {
                mode = ExecutionMode.LOCAL;
            }
            functionDeclarationContext.addVariable(
                name,
                expression.getParams().get(name),
                expression.getMetadata(),
                mode
            );
            ++i;
        }
    }

    @Override
    public StaticContext visitFunctionDeclaration(FunctionDeclaration declaration, StaticContext argument) {
        InlineFunctionExpression expression = (InlineFunctionExpression) declaration.getExpression();
        // define a static context for the function body, add params to the context and visit the body expression
        List<ExecutionMode> modes = Functions.getUserDefinedFunctionParametersStorageMode(
            expression.getFunctionIdentifier(),
            expression.getMetadata()
        );
        StaticContext functionDeclarationContext = new StaticContext(argument);
        populateFunctionDeclarationStaticContext(functionDeclarationContext, modes, expression);
        // visit the body first to make its execution mode available while adding the function to the catalog
        this.visit(expression.getBody(), functionDeclarationContext);
        expression.initHighestExecutionMode(this.visitorConfig);
        declaration.initHighestExecutionMode(this.visitorConfig);
        expression.registerUserDefinedFunctionExecutionMode(
            this.visitorConfig
        );
        return functionDeclarationContext;
    }

    @Override
    public StaticContext visitInlineFunctionExpr(InlineFunctionExpression expression, StaticContext argument) {
        // define a static context for the function body, add params to the context and visit the body expression
        StaticContext functionDeclarationContext = new StaticContext(argument);
        expression.getParams()
            .forEach(
                (paramName, sequenceType) -> functionDeclarationContext.addVariable(
                    paramName,
                    sequenceType,
                    expression.getMetadata(),
                    ExecutionMode.LOCAL
                )
            );
        // visit the body first to make its execution mode available while adding the function to the catalog
        this.visit(expression.getBody(), functionDeclarationContext);
        expression.initHighestExecutionMode(this.visitorConfig);
        expression.registerUserDefinedFunctionExecutionMode(
            this.visitorConfig
        );
        return functionDeclarationContext;
    }

    @Override
    public StaticContext visitFunctionCall(FunctionCallExpression expression, StaticContext argument) {
        visitDescendants(expression, argument);
        FunctionIdentifier identifier = expression.getFunctionIdentifier();
        if (!BuiltinFunctionCatalogue.exists(identifier)) {
            List<ExecutionMode> modes = new ArrayList<>();
            if (expression.isPartialApplication()) {
                for (@SuppressWarnings("unused")
                Expression parameter : expression.getArguments()) {
                    modes.add(ExecutionMode.LOCAL);
                }
            } else {
                for (Expression parameter : expression.getArguments()) {
                    modes.add(parameter.getHighestExecutionMode(this.visitorConfig));
                }
            }
            Functions.addUserDefinedFunctionParametersStorageMode(
                identifier,
                modes,
                this.visitorConfig.suppressErrorsForFunctionSignatureCollision(),
                expression.getMetadata()
            );
        }
        expression.initFunctionCallHighestExecutionMode(this.visitorConfig);
        return argument;
    }
    // endregion

    // region FLWOR
    @Override
    public StaticContext visitFlowrExpression(FlworExpression expression, StaticContext argument) {
        Clause clause = expression.getReturnClause().getFirstClause();
        StaticContext result = this.visit(clause, argument);
        while (clause != null) {
            result = this.visit(clause, result);
            clause = clause.getNextClause();
        }
        expression.initHighestExecutionMode(this.visitorConfig);
        return argument;
    }

    // region FLWOR vars
    @Override
    public StaticContext visitForClause(ForClause clause, StaticContext argument) {
        // TODO visit at...
        this.visit(clause.getExpression(), argument);
        clause.initHighestExecutionMode(this.visitorConfig);

        StaticContext result = new StaticContext(argument);
        result.addVariable(
            clause.getVariableName(),
            clause.getSequenceType(),
            clause.getMetadata(),
            clause.getVariableHighestStorageMode(this.visitorConfig)
        );

        if (clause.getPositionalVariableName() != null) {
            result.addVariable(
                clause.getPositionalVariableName(),
                new SequenceType(ItemType.integerItem),
                clause.getMetadata(),
                ExecutionMode.LOCAL
            );
        }
        return result;
    }

    @Override
    public StaticContext visitLetClause(LetClause clause, StaticContext argument) {
        this.visit(clause.getExpression(), argument);
        clause.initHighestExecutionMode(this.visitorConfig);

        StaticContext result = new StaticContext(argument);
        result.addVariable(
            clause.getVariableName(),
            clause.getSequenceType(),
            clause.getMetadata(),
            clause.getVariableHighestStorageMode(this.visitorConfig)
        );

        return result;
    }

    @Override
    public StaticContext visitGroupByClause(GroupByClause clause, StaticContext argument) {
        StaticContext groupByClauseContext = new StaticContext(argument);
        for (GroupByVariableDeclaration variable : clause.getGroupVariables()) {
            if (variable.getExpression() != null) {
                // if a variable declaration takes place
                this.visit(variable.getExpression(), argument);
                groupByClauseContext.addVariable(
                    variable.getVariableName(),
                    variable.getSequenceType(),
                    clause.getMetadata(),
                    ExecutionMode.LOCAL
                );
            } else if (!argument.isInScope(variable.getVariableName())) {
                throw new UndeclaredVariableException(
                        "Uninitialized variable reference: " + variable.getVariableName(),
                        clause.getMetadata()
                );
            }
        }
        clause.initHighestExecutionMode(this.visitorConfig);
        return groupByClauseContext;
    }

    @Override
    public StaticContext visitCountClause(CountClause expression, StaticContext argument) {
        expression.initHighestExecutionMode(this.visitorConfig);
        StaticContext result = new StaticContext(argument);
        result.addVariable(
            expression.getCountVariable().getVariableName(),
            new SequenceType(ItemType.integerItem, SequenceType.Arity.One),
            expression.getMetadata(),
            ExecutionMode.LOCAL
        );
        this.visit(expression.getCountVariable(), result);
        return result;
    }

    // endregion

    // region quantifiers
    @Override
    public StaticContext visitQuantifiedExpression(QuantifiedExpression expression, StaticContext argument) {
        StaticContext contextWithQuantifiedExpressionVariables = argument;
        for (QuantifiedExpressionVar clause : expression.getVariables()) {
            this.visit(clause.getExpression(), contextWithQuantifiedExpressionVariables);
            expression.initHighestExecutionMode(this.visitorConfig);

            // create a child context, add the variable and return it
            StaticContext result = new StaticContext(contextWithQuantifiedExpressionVariables);
            result.addVariable(
                clause.getVariableName(),
                clause.getSequenceType(),
                expression.getMetadata(),
                ExecutionMode.LOCAL
            );
            contextWithQuantifiedExpressionVariables = result;
        }
        // validate expression with the defined variables
        this.visit(expression.getEvaluationExpression(), contextWithQuantifiedExpressionVariables);
        expression.initHighestExecutionMode(this.visitorConfig);
        // return the given context unchanged as defined variables go out of scope
        return argument;
    }
    // endregion

    // region control
    @Override
    public StaticContext visitTypeSwitchExpression(TypeSwitchExpression expression, StaticContext argument) {
        this.visit(expression.getTestCondition(), argument);
        for (TypeswitchCase c : expression.getCases()) {
            StaticContext caseContext = new StaticContext(argument);
            Name variableName = c.getVariableName();
            if (variableName != null) {
                caseContext.addVariable(
                    variableName,
                    null,
                    expression.getMetadata(),
                    ExecutionMode.LOCAL
                );
            }
            this.visit(c.getReturnExpression(), caseContext);
        }

        Name defaultCaseVariableName = expression.getDefaultCase().getVariableName();
        if (defaultCaseVariableName == null) {
            this.visit(expression.getDefaultCase().getReturnExpression(), argument);
        } else {
            // add variable to child context to visit default return expression
            StaticContext defaultCaseStaticContext = new StaticContext(argument);
            defaultCaseStaticContext.addVariable(
                defaultCaseVariableName,
                null,
                expression.getMetadata(),
                ExecutionMode.LOCAL
            );
            this.visit(expression.getDefaultCase().getReturnExpression(), defaultCaseStaticContext);
        }
        expression.initHighestExecutionMode(this.visitorConfig);
        // return the given context unchanged as defined variables go out of scope
        return argument;
    }
    // endregion

    @Override
    public StaticContext visitVariableDeclaration(VariableDeclaration variableDeclaration, StaticContext argument) {
        if (variableDeclaration.getExpression() != null) {
            this.visit(variableDeclaration.getExpression(), argument);
        }
        variableDeclaration.initHighestExecutionMode(this.visitorConfig);
        if (argument.hasVariable(variableDeclaration.getVariableName())) {
            throw new VariableAlreadyExistsException(
                    variableDeclaration.getVariableName(),
                    variableDeclaration.getMetadata()
            );
        }
        StaticContext result = new StaticContext(argument);
        result.addVariable(
            variableDeclaration.getVariableName(),
            variableDeclaration.getSequenceType(),
            variableDeclaration.getMetadata(),
            variableDeclaration.getVariableHighestStorageMode(this.visitorConfig)
        );
        return result;
    }

    public StaticContext processImportedModule(LibraryModule libraryModule, StaticContext argument) {
        StaticContext moduleContext = libraryModule.getStaticContext();
        argument.importModuleContext(moduleContext, libraryModule.getNamespace());
        return argument;
    }

}
