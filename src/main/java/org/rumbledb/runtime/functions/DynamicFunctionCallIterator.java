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

package org.rumbledb.runtime.functions;

import org.rumbledb.api.Item;
import org.rumbledb.context.DynamicContext;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.UnexpectedTypeException;
import org.rumbledb.items.FunctionItem;
import org.rumbledb.runtime.LocalRuntimeIterator;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.runtime.functions.base.Functions;
import sparksoniq.jsoniq.ExecutionMode;

import java.util.List;

public class DynamicFunctionCallIterator extends LocalRuntimeIterator {
    // dynamic: functionIdentifier is not known at compile time
    // it is known only after evaluating postfix expression at runtime

    private static final long serialVersionUID = 1L;
    // parametrized fields
    private RuntimeIterator functionItemIterator;
    private List<RuntimeIterator> functionArguments;

    // calculated fields
    private FunctionItem functionItem;
    private RuntimeIterator functionCallIterator;
    private Item nextResult;

    public DynamicFunctionCallIterator(
            RuntimeIterator functionItemIterator,
            List<RuntimeIterator> functionArguments,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(null, executionMode, iteratorMetadata);
        for (RuntimeIterator arg : functionArguments) {
            if (arg != null) {
                this.children.add(arg);
            }
        }
        if (!this.children.contains(functionItemIterator)) {
            this.children.add(functionItemIterator);
        }
        this.functionItemIterator = functionItemIterator;
        this.functionArguments = functionArguments;
    }

    @Override
    public void open(DynamicContext context) {
        super.open(context);
        setFunctionItemAndIteratorWithCurrentContext();
        this.functionCallIterator.open(this.currentDynamicContextForLocalExecution);
        setNextResult();
    }

    @Override
    public Item next() {
        if (this.hasNext) {
            Item result = this.nextResult;
            setNextResult();
            return result;
        }
        throw new IteratorFlowException(
                RuntimeIterator.FLOW_EXCEPTION_MESSAGE
                    + " in "
                    + this.functionItem.getIdentifier().getName()
                    + "  function",
                getMetadata()
        );
    }

    public void setNextResult() {
        this.nextResult = null;
        if (this.functionCallIterator.hasNext()) {
            this.nextResult = this.functionCallIterator.next();
        }

        if (this.nextResult == null) {
            this.hasNext = false;
            this.functionCallIterator.close();
        } else {
            this.hasNext = true;
        }
    }

    private void setFunctionItemAndIteratorWithCurrentContext() {
        try {
            this.functionItemIterator.open(this.currentDynamicContextForLocalExecution);
            if (this.functionItemIterator.hasNext()) {
                this.functionItem = (FunctionItem) this.functionItemIterator.next();
            }
            if (this.functionItemIterator.hasNext()) {
                throw new UnexpectedTypeException(
                        "Dynamic function call can not be performed on a sequence.",
                        getMetadata()
                );
            }
            this.functionItemIterator.close();
        } catch (ClassCastException e) {
            throw new UnexpectedTypeException(
                    "Dynamic function call can only be performed on functions.",
                    getMetadata()
            );
        }
        this.functionCallIterator = Functions.buildUserDefinedFunctionCallIterator(
            this.functionItem,
            this.functionItem.getBodyIterator().getHighestExecutionMode(),
            getMetadata(),
            this.functionArguments
        );
    }

    @Override
    public void reset(DynamicContext context) {
        super.reset(context);
        this.functionCallIterator.reset(this.currentDynamicContextForLocalExecution);
        setNextResult();
    }

    @Override
    public void close() {
        // ensure that recursive function calls terminate gracefully
        // the function call in the body of the deepest recursion call is never visited, never opened and never closed
        if (this.isOpen) {
            this.functionCallIterator.close();
        }
        super.close();
    }
}
