/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: Stefan Irimescu
 *
 */
 package jiqs.jsoniq.runtime.iterator.operational.base;

import jiqs.jsoniq.compiler.translator.expr.operational.base.OperationalExpressionBase;
import jiqs.jsoniq.runtime.iterator.LocalRuntimeIterator;
import jiqs.jsoniq.runtime.iterator.RuntimeIterator;

import java.util.ArrayList;

public abstract class UnaryOperationBaseIterator extends LocalRuntimeIterator {

    protected final RuntimeIterator _child;
    protected final OperationalExpressionBase.Operator _operator;

    protected UnaryOperationBaseIterator(RuntimeIterator child, OperationalExpressionBase.Operator operator) {
        super(new ArrayList<>());
        this._children.add(child);
        this._child = child;
        this._operator = operator;
    }
}
