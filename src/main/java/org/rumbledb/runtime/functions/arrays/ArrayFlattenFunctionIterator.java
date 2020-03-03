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

package org.rumbledb.runtime.functions.arrays;

import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.runtime.functions.base.LocalFunctionCallIterator;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.semantics.DynamicContext;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ArrayFlattenFunctionIterator extends LocalFunctionCallIterator {

    private static final long serialVersionUID = 1L;

    private RuntimeIterator iterator;
    private Queue<Item> nextResults; // queue that holds the results created by the current item in inspection

    public ArrayFlattenFunctionIterator(
            List<RuntimeIterator> arguments,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(arguments, executionMode, iteratorMetadata);
    }

    @Override
    public Item next() {
        if (this.hasNext) {
            Item result = this.nextResults.remove(); // save the result to be returned
            if (this.nextResults.isEmpty()) {
                // if there are no more results left in the queue, trigger calculation for the next result
                setNextResult();
            }
            return result;
        }
        throw new IteratorFlowException(
                RuntimeIterator.FLOW_EXCEPTION_MESSAGE + " FLATTEN function",
                getMetadata()
        );
    }

    @Override
    public void open(DynamicContext context) {
        super.open(context);

        this.iterator = this.children.get(0);
        this.iterator.open(context);
        this.nextResults = new LinkedList<>();

        setNextResult();
    }

    public void setNextResult() {
        while (this.iterator.hasNext()) {
            Item item = this.iterator.next();
            flatten(Collections.singletonList(item));
            if (!(this.nextResults.isEmpty())) {
                break;
            }
        }
        if (this.nextResults.isEmpty()) {
            this.hasNext = false;
            this.iterator.close();
        } else {
            this.hasNext = true;
        }
    }

    private void flatten(List<Item> items) {
        for (Item item : items) {
            if (item.isArray()) {
                flatten(item.getItems());
            } else {
                this.nextResults.add(item);
            }
        }
    }
}
