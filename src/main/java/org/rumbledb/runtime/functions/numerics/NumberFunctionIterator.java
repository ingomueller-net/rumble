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

package org.rumbledb.runtime.functions.numerics;

import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.items.AtomicItem;
import org.rumbledb.items.ItemFactory;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.runtime.functions.base.LocalFunctionCallIterator;
import sparksoniq.jsoniq.ExecutionMode;
import org.rumbledb.types.ItemType;

import java.util.List;

public class NumberFunctionIterator extends LocalFunctionCallIterator {

    private static final long serialVersionUID = 1L;

    public NumberFunctionIterator(
            List<RuntimeIterator> parameters,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(parameters, executionMode, iteratorMetadata);
    }

    @Override
    public Item next() {
        if (this.hasNext) {
            this.hasNext = false;
            Item anyItem = this.children.get(0).materializeFirstItemOrNull(this.currentDynamicContextForLocalExecution);
            if (anyItem == null) {
                return ItemFactory.getInstance().createDoubleItem(Double.NaN);
            }

            AtomicItem atomicItem = (AtomicItem) anyItem;
            if (atomicItem.isCastableAs(ItemType.doubleItem)) {
                try {
                    return atomicItem.castAs(ItemType.doubleItem);
                } catch (ClassCastException e) {
                    return ItemFactory.getInstance().createDoubleItem(Double.NaN);
                }
            }
            return ItemFactory.getInstance().createDoubleItem(Double.NaN);
        } else
            throw new IteratorFlowException(
                    RuntimeIterator.FLOW_EXCEPTION_MESSAGE + " number function",
                    getMetadata()
            );
    }
}
