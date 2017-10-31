/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.gaffer.store.operation.handler;

import uk.gov.gchq.gaffer.store.operation.add.AddSchema;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.library.GraphLibrary;
import uk.gov.gchq.gaffer.store.schema.Schema;

import static uk.gov.gchq.gaffer.store.library.GraphLibrary.*;

public class AddSchemaHandler implements OperationHandler<AddSchema> {

    public static final String ERROR_ADDING_SCHEMA_TO_STORE_S = "Error adding schema to Store,%s";
    public static final String THE_STORE_DOES_NOT_HAVE_A_GRAPH_LIBRARY = " the store doesn't have a graphLibrary";

    @Override
    public Void doOperation(final AddSchema operation, final Context context, final Store store) throws OperationException {
        GraphLibrary graphLibrary = store.getGraphLibrary();
        if (null == graphLibrary) {
            throw new OperationException(String.format(ERROR_ADDING_SCHEMA_TO_STORE_S, THE_STORE_DOES_NOT_HAVE_A_GRAPH_LIBRARY));
        } else {
            Schema mergedSchema;
            try {
                mergedSchema = resolveSchema(store, operation.getSchema(), operation.getParentSchemaIds());
            } catch (final Exception e) {
                throw new OperationException(String.format(ERROR_ADDING_SCHEMA_TO_STORE_S, " schema couldn't be resolved."), e);
            }
            try {
                graphLibrary.addSchema(mergedSchema);
            } catch (final Exception e) {
                throw new OperationException(String.format(ERROR_ADDING_SCHEMA_TO_STORE_S, " schema: " + operation.getSchema()), e);
            }
        }
        return null;
    }
}
