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

package org.rumbledb.expressions;

import org.rumbledb.expressions.arithmetic.AdditiveExpression;
import org.rumbledb.expressions.arithmetic.MultiplicativeExpression;
import org.rumbledb.expressions.arithmetic.UnaryExpression;
import org.rumbledb.expressions.comparison.ComparisonExpression;
import org.rumbledb.expressions.control.ConditionalExpression;
import org.rumbledb.expressions.control.SwitchExpression;
import org.rumbledb.expressions.control.TryCatchExpression;
import org.rumbledb.expressions.control.TypeSwitchExpression;
import org.rumbledb.expressions.flowr.CountClause;
import org.rumbledb.expressions.flowr.FlworExpression;
import org.rumbledb.expressions.flowr.ForClause;
import org.rumbledb.expressions.flowr.GroupByClause;
import org.rumbledb.expressions.flowr.LetClause;
import org.rumbledb.expressions.flowr.OrderByClause;
import org.rumbledb.expressions.flowr.ReturnClause;
import org.rumbledb.expressions.flowr.SimpleMapExpression;
import org.rumbledb.expressions.flowr.WhereClause;
import org.rumbledb.expressions.logic.AndExpression;
import org.rumbledb.expressions.logic.NotExpression;
import org.rumbledb.expressions.logic.OrExpression;
import org.rumbledb.expressions.miscellaneous.RangeExpression;
import org.rumbledb.expressions.miscellaneous.StringConcatExpression;
import org.rumbledb.expressions.module.FunctionDeclaration;
import org.rumbledb.expressions.module.LibraryModule;
import org.rumbledb.expressions.module.MainModule;
import org.rumbledb.expressions.module.Prolog;
import org.rumbledb.expressions.module.VariableDeclaration;
import org.rumbledb.expressions.postfix.ArrayLookupExpression;
import org.rumbledb.expressions.postfix.ArrayUnboxingExpression;
import org.rumbledb.expressions.postfix.DynamicFunctionCallExpression;
import org.rumbledb.expressions.postfix.ObjectLookupExpression;
import org.rumbledb.expressions.postfix.PredicateExpression;
import org.rumbledb.expressions.primary.ArrayConstructorExpression;
import org.rumbledb.expressions.primary.BooleanLiteralExpression;
import org.rumbledb.expressions.primary.ContextItemExpression;
import org.rumbledb.expressions.primary.DecimalLiteralExpression;
import org.rumbledb.expressions.primary.DoubleLiteralExpression;
import org.rumbledb.expressions.primary.FunctionCallExpression;
import org.rumbledb.expressions.primary.InlineFunctionExpression;
import org.rumbledb.expressions.primary.IntegerLiteralExpression;
import org.rumbledb.expressions.primary.NamedFunctionReferenceExpression;
import org.rumbledb.expressions.primary.NullLiteralExpression;
import org.rumbledb.expressions.primary.ObjectConstructorExpression;
import org.rumbledb.expressions.primary.StringLiteralExpression;
import org.rumbledb.expressions.primary.VariableReferenceExpression;
import org.rumbledb.expressions.quantifiers.QuantifiedExpression;
import org.rumbledb.expressions.typing.CastExpression;
import org.rumbledb.expressions.typing.CastableExpression;
import org.rumbledb.expressions.typing.InstanceOfExpression;
import org.rumbledb.expressions.typing.TreatExpression;

public abstract class AbstractNodeVisitor<T> {

    public T visit(Node node, T argument) {
        return node.accept(this, argument);
    }

    public T visitDescendants(Node node, T argument) {
        T result = argument;
        for (Node child : node.getChildren()) {
            result = visit(child, result);
        }
        return result;
    }

    protected T defaultAction(Node node, T argument) {
        return visitDescendants(node, argument);
    }

    public T visitCommaExpression(CommaExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    // region module
    public T visitMainModule(MainModule expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitLibraryModule(LibraryModule expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitProlog(Prolog expression, T argument) {
        return defaultAction(expression, argument);
    }

    // endregion

    // region flwor
    public T visitFlowrExpression(FlworExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitVariableReference(VariableReferenceExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitForClause(ForClause expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitLetClause(LetClause expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitGroupByClause(GroupByClause expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitOrderByClause(OrderByClause expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitWhereClause(WhereClause expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitCountClause(CountClause expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitReturnClause(ReturnClause expression, T argument) {
        return defaultAction(expression, argument);
    }
    // endregion

    // region postfix
    public T visitArrayUnboxingExpression(ArrayUnboxingExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitArrayLookupExpression(ArrayLookupExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitObjectLookupExpression(ObjectLookupExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitPredicateExpression(PredicateExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitDynamicFunctionCallExpression(DynamicFunctionCallExpression expression, T argument) {
        return defaultAction(expression, argument);
    }
    // endregion

    // region primary
    public T visitArrayConstructor(ArrayConstructorExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitObjectConstructor(ObjectConstructorExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitContextExpr(ContextItemExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitFunctionCall(FunctionCallExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitInlineFunctionExpr(InlineFunctionExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitNamedFunctionRef(NamedFunctionReferenceExpression expression, T argument) {
        return defaultAction(expression, argument);
    }
    // endregion

    // region literal
    public T visitInteger(IntegerLiteralExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitString(StringLiteralExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitDouble(DoubleLiteralExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitDecimal(DecimalLiteralExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitNull(NullLiteralExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitBoolean(BooleanLiteralExpression expression, T argument) {
        return defaultAction(expression, argument);
    }
    // endregion

    // region operational
    public T visitAdditiveExpr(AdditiveExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitMultiplicativeExpr(MultiplicativeExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitSimpleMapExpr(SimpleMapExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitAndExpr(AndExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitOrExpr(OrExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitNotExpr(NotExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitUnaryExpr(UnaryExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitRangeExpr(RangeExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitStringConcatExpr(StringConcatExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitComparisonExpr(ComparisonExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitInstanceOfExpression(InstanceOfExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitTreatExpression(TreatExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitCastableExpression(CastableExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitCastExpression(CastExpression expression, T argument) {
        return defaultAction(expression, argument);
    }
    // endregion

    // region quantifiers
    public T visitQuantifiedExpression(QuantifiedExpression expression, T argument) {
        return defaultAction(expression, argument);
    }
    // endregion

    // region control
    public T visitConditionalExpression(ConditionalExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitSwitchExpression(SwitchExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitTypeSwitchExpression(TypeSwitchExpression expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitTryCatchExpression(TryCatchExpression expression, T argument) {
        return defaultAction(expression, argument);
    }
    // endregion

    // region prolog
    public T visitVariableDeclaration(VariableDeclaration expression, T argument) {
        return defaultAction(expression, argument);
    }

    public T visitFunctionDeclaration(FunctionDeclaration expression, T argument) {
        return defaultAction(expression, argument);
    }
    // endregion

}
