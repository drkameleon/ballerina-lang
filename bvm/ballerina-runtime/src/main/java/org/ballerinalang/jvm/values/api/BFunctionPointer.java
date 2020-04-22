/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.jvm.values.api;

import org.ballerinalang.jvm.values.FutureValue;

import java.util.function.Function;

/**
 * <p>
 * Ballerina runtime value representation of a function pointer.
 * </p>
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 *
 * @since 1.1.0
 */
public interface BFunctionPointer<T, R> extends BRefValue {

    /**
     * Execute the {@code Function} with given parameter array.
     *
     * @param t {@code Function to be executed}
     * @return The result of the executed function.
     */
    R call(T t);

    /**
     * Execute the {@code Function} with given parameter array.
     *
     * @param args Function arguments.
     * @return Future value received from invoking asynchronous function.
     */
    FutureValue asyncCall(Object[] args);

    /**
     * Execute the {@code Function} with given parameter array.
     *
     * @param args                 Function arguments.
     * @param resultHandleFunction Function used to process the result received after execution of function.
     * @return Future value received from invoking asynchronous function.
     */
    FutureValue asyncCall(Object[] args, Function<Object, Object> resultHandleFunction);

    /**
     * Returns the {@code Function} the FP is pointed to.
     *
     * @return {@code Function}
     */
    Function<T, R> getFunction();
}
