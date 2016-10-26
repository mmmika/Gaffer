/*
 * Copyright 2016 Crown Copyright
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
package gaffer.accumulostore.operation.spark.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;
import gaffer.data.element.Edge;
import gaffer.data.element.Element;
import gaffer.data.element.Entity;
import gaffer.data.element.function.ElementFilter;
import gaffer.data.elementdefinition.view.View;
import gaffer.data.elementdefinition.view.ViewElementDefinition;
import gaffer.function.simple.filter.IsMoreThan;
import gaffer.graph.Graph;
import gaffer.operation.OperationException;
import gaffer.operation.impl.add.AddElements;
import gaffer.operation.simple.spark.ConversionException;
import gaffer.operation.simple.spark.Converter;
import gaffer.operation.simple.spark.GetDataFrameOfElements;
import gaffer.types.simple.FreqMap;
import gaffer.user.User;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.Row$;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.junit.Test;

import java.io.Serializable;
import java.util.*;

public class GetDataFrameOfElementsHandlerTest {

    private final static String ENTITY_GROUP = "BasicEntity";
    private final static String EDGE_GROUP = "BasicEdge";
    private final static int NUM_ELEMENTS = 10;

    @Test
    public void checkGetCorrectElementsInDataFrame() throws OperationException {
        final Graph graph1 = new Graph.Builder()
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataSchema.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/storeTypes.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final User user = new User();
        graph1.execute(new AddElements(getElements()), user);

        final SparkConf sparkConf = new SparkConf()
                .setMaster("local")
                .setAppName("checkGetCorrectElementsInDataFrame")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrator", "gaffer.serialisation.kryo.Registrator")
                .set("spark.driver.allowMultipleContexts", "true");
        final SparkContext sparkContext = new SparkContext(sparkConf);
        final SQLContext sqlContext = new SQLContext(sparkContext);

        // Edges group - check get correct edges
        GetDataFrameOfElements dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().edge(EDGE_GROUP).build())
                .build();
        Dataset<Row> dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }
        Set<Row> results = new HashSet<>(dataFrame.collectAsList());
        final Set<Row> expectedRows = new HashSet<>();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            final scala.collection.mutable.MutableList<Object> fields1 = new scala.collection.mutable.MutableList<>();
            fields1.appendElem(EDGE_GROUP);
            fields1.appendElem("" + i);
            fields1.appendElem("B");
            fields1.appendElem(1);
            fields1.appendElem(2);
            fields1.appendElem(3.0F);
            fields1.appendElem(4.0D);
            fields1.appendElem(5L);
            fields1.appendElem(100L);
            expectedRows.add(Row$.MODULE$.fromSeq(fields1));
            final scala.collection.mutable.MutableList<Object> fields2 = new scala.collection.mutable.MutableList<>();
            fields2.appendElem(EDGE_GROUP);
            fields2.appendElem("" + i);
            fields2.appendElem("C");
            fields2.appendElem(6);
            fields2.appendElem(7);
            fields2.appendElem(8.0F);
            fields2.appendElem(9.0D);
            fields2.appendElem(10L);
            fields2.appendElem(i * 200L);
            expectedRows.add(Row$.MODULE$.fromSeq(fields2));
        }
        assertEquals(expectedRows, results);

        // Entities group - check get correct entities
        dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().entity(ENTITY_GROUP).build())
                .build();
        dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }
        results.clear();
        results.addAll(dataFrame.collectAsList());
        expectedRows.clear();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            final scala.collection.mutable.MutableList<Object> fields1 = new scala.collection.mutable.MutableList<>();
            fields1.clear();
            fields1.appendElem(ENTITY_GROUP);
            fields1.appendElem("" + i);
            fields1.appendElem(1);
            fields1.appendElem(i);
            fields1.appendElem(3.0F);
            fields1.appendElem(4.0D);
            fields1.appendElem(5L);
            fields1.appendElem(6);
            expectedRows.add(Row$.MODULE$.fromSeq(fields1));
        }
        assertEquals(expectedRows, results);

        sparkContext.stop();
    }

    @Test
    public void checkGetCorrectElementsInDataFrameMultipleGroups() throws OperationException {
        final Graph graph1 = new Graph.Builder()
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataSchema.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/storeTypes.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final User user = new User();
        graph1.execute(new AddElements(getElements()), user);

        final SparkConf sparkConf = new SparkConf()
                .setMaster("local")
                .setAppName("checkGetCorrectElementsInDataFrameMultipleGroups")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrator", "gaffer.serialisation.kryo.Registrator")
                .set("spark.driver.allowMultipleContexts", "true");
        final SparkContext sparkContext = new SparkContext(sparkConf);
        final SQLContext sqlContext = new SQLContext(sparkContext);

        // Use entity and edges group - check get correct data
        GetDataFrameOfElements dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().entity(ENTITY_GROUP).edge(EDGE_GROUP).build())
                .build();
        Dataset<Row> dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }
        final Set<Row> results = new HashSet<>(dataFrame.collectAsList());
        final Set<Row> expectedRows = new HashSet<>();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            final scala.collection.mutable.MutableList<Object> fields1 = new scala.collection.mutable.MutableList<>();
            fields1.appendElem(EDGE_GROUP);
            fields1.appendElem(null);
            fields1.appendElem(1);
            fields1.appendElem(2);
            fields1.appendElem(3.0F);
            fields1.appendElem(4.0D);
            fields1.appendElem(5L);
            fields1.appendElem(100L);
            fields1.appendElem("" + i);
            fields1.appendElem("B");
            expectedRows.add(Row$.MODULE$.fromSeq(fields1));
            final scala.collection.mutable.MutableList<Object> fields2 = new scala.collection.mutable.MutableList<>();
            fields2.appendElem(EDGE_GROUP);
            fields2.appendElem(null);
            fields2.appendElem(6);
            fields2.appendElem(7);
            fields2.appendElem(8.0F);
            fields2.appendElem(9.0D);
            fields2.appendElem(10L);
            fields2.appendElem(i * 200L);
            fields2.appendElem("" + i);
            fields2.appendElem("C");
            expectedRows.add(Row$.MODULE$.fromSeq(fields2));
            final scala.collection.mutable.MutableList<Object> fields3 = new scala.collection.mutable.MutableList<>();
            fields3.appendElem(ENTITY_GROUP);
            fields3.appendElem("" + i);
            fields3.appendElem(1);
            fields3.appendElem(i);
            fields3.appendElem(3.0F);
            fields3.appendElem(4.0D);
            fields3.appendElem(5L);
            fields3.appendElem(6);
            fields3.appendElem(null);
            fields3.appendElem(null);
            expectedRows.add(Row$.MODULE$.fromSeq(fields3));
        }
        assertEquals(expectedRows, results);

        // Entities group - check get correct entities
        dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().entity(ENTITY_GROUP).build())
                .build();
        dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }
        results.clear();
        results.addAll(dataFrame.collectAsList());
        expectedRows.clear();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            final scala.collection.mutable.MutableList<Object> fields1 = new scala.collection.mutable.MutableList<>();
            fields1.clear();
            fields1.appendElem(ENTITY_GROUP);
            fields1.appendElem("" + i);
            fields1.appendElem(1);
            fields1.appendElem(i);
            fields1.appendElem(3.0F);
            fields1.appendElem(4.0D);
            fields1.appendElem(5L);
            fields1.appendElem(6);
            expectedRows.add(Row$.MODULE$.fromSeq(fields1));
        }
        assertEquals(expectedRows, results);

        sparkContext.stop();
    }

    @Test
    public void checkGetCorrectElementsInDataFrameWithProjection() throws OperationException {
        final Graph graph1 = new Graph.Builder()
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataSchema.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/storeTypes.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final User user = new User();
        graph1.execute(new AddElements(getElements()), user);

        final SparkConf sparkConf = new SparkConf()
                .setMaster("local")
                .setAppName("checkGetCorrectElementsInDataFrameWithProjection")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrator", "gaffer.serialisation.kryo.Registrator")
                .set("spark.driver.allowMultipleContexts", "true");
        final SparkContext sparkContext = new SparkContext(sparkConf);
        final SQLContext sqlContext = new SQLContext(sparkContext);

        // Get all edges
        final GetDataFrameOfElements dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().edge(EDGE_GROUP).build())
                .build();
        final Dataset<Row> dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }

        // Check get correct rows when ask for src, dst and property2 columns
        Set<Row> results = new HashSet<>(dataFrame.select("src", "dst", "property2").collectAsList());
        final Set<Row> expectedRows = new HashSet<>();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            scala.collection.mutable.MutableList<Object> fields1 = new scala.collection.mutable.MutableList<>();
            fields1.appendElem("" + i);
            fields1.appendElem("B");
            fields1.appendElem(3.0F);
            expectedRows.add(Row$.MODULE$.fromSeq(fields1));
            scala.collection.mutable.MutableList<Object> fields2 = new scala.collection.mutable.MutableList<>();
            fields2.appendElem("" + i);
            fields2.appendElem("C");
            fields2.appendElem(8.0F);
            expectedRows.add(Row$.MODULE$.fromSeq(fields2));
        }
        assertEquals(expectedRows, results);

        // Check get correct rows when ask for just property2 column
        results = new HashSet<>(dataFrame.select("property2").collectAsList());
        expectedRows.clear();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            scala.collection.mutable.MutableList<Object> fields1 = new scala.collection.mutable.MutableList<>();
            fields1.appendElem(3.0F);
            expectedRows.add(Row$.MODULE$.fromSeq(fields1));
            scala.collection.mutable.MutableList<Object> fields2 = new scala.collection.mutable.MutableList<>();
            fields2.appendElem(8.0F);
            expectedRows.add(Row$.MODULE$.fromSeq(fields2));
        }
        assertEquals(expectedRows, results);

        sparkContext.stop();
    }

    @Test
    public void checkGetCorrectElementsInDataFrameWithProjectionAndFiltering() throws OperationException {
        final Graph graph1 = new Graph.Builder()
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataSchema.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/storeTypes.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final User user = new User();
        graph1.execute(new AddElements(getElements()), user);

        final SparkConf sparkConf = new SparkConf()
                .setMaster("local")
                .setAppName("checkGetCorrectElementsInDataFrameWithProjectionAndFiltering")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrator", "gaffer.serialisation.kryo.Registrator")
                .set("spark.driver.allowMultipleContexts", "true");
        final SparkContext sparkContext = new SparkContext(sparkConf);
        final SQLContext sqlContext = new SQLContext(sparkContext);

        // Get DataFrame
        final GetDataFrameOfElements dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().edge(EDGE_GROUP).build())
                .build();
        final Dataset<Row> dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }

        // Check get correct rows when ask for all columns but only rows where property2 > 4.0
        Set<Row> results = new HashSet<>(dataFrame.filter("property2 > 4.0").collectAsList());
        final Set<Row> expectedRows = new HashSet<>();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            final scala.collection.mutable.MutableList<Object> fields = new scala.collection.mutable.MutableList<>();
            fields.appendElem(EDGE_GROUP);
            fields.appendElem("" + i);
            fields.appendElem("C");
            fields.appendElem(6);
            fields.appendElem(7);
            fields.appendElem(8.0F);
            fields.appendElem(9.0D);
            fields.appendElem(10L);
            fields.appendElem(i * 200L);
            expectedRows.add(Row$.MODULE$.fromSeq(fields));
        }
        assertEquals(expectedRows, results);

        // Check get correct rows when ask for columns property2 and property3 but only rows where property2 > 4.0
        results = new HashSet<>(dataFrame.select("property2", "property3").filter("property2 > 4.0").collectAsList());
        expectedRows.clear();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            final scala.collection.mutable.MutableList<Object> fields = new scala.collection.mutable.MutableList<>();
            fields.appendElem(8.0F);
            fields.appendElem(9.0D);
            expectedRows.add(Row$.MODULE$.fromSeq(fields));
        }
        assertEquals(expectedRows, results);

        sparkContext.stop();
    }

    @Test
    public void checkGetExceptionIfIncompatibleSchemas() throws OperationException {
        final Graph graph1 = new Graph.Builder()
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataSchemaIncompatible.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/storeTypes.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final User user = new User();

        final SparkConf sparkConf = new SparkConf()
                .setMaster("local")
                .setAppName("checkGetExceptionIfIncompatibleSchemas")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrator", "gaffer.serialisation.kryo.Registrator")
                .set("spark.driver.allowMultipleContexts", "true");
        final SparkContext sparkContext = new SparkContext(sparkConf);
        final SQLContext sqlContext = new SQLContext(sparkContext);

        // Use entity and edges group - check get correct data
        final GetDataFrameOfElements dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().entity(ENTITY_GROUP).edge(EDGE_GROUP).build())
                .build();
        // NB Catch the exception rather than using expected annotation on test to ensure that the SparkContext
        // is shut down.
        try {
            graph1.execute(dfOperation, user);
            fail("IllegalArgumentException should have been thrown");
        } catch (final IllegalArgumentException e) {
            // Expected
        }
        sparkContext.stop();
    }

    @Test
    public void checkCanDealWithNonStandardProperties() throws OperationException {
        final Graph graph1 = new Graph.Builder()
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataSchemaNonstandardTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/storeTypes.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final User user = new User();
        graph1.execute(new AddElements(getElementsWithNonStandardProperties()), user);

        final SparkConf sparkConf = new SparkConf()
                .setMaster("local")
                .setAppName("checkCanDealWithNonStandardProperties")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrator", "gaffer.serialisation.kryo.Registrator")
                .set("spark.driver.allowMultipleContexts", "true");
        final SparkContext sparkContext = new SparkContext(sparkConf);
        final SQLContext sqlContext = new SQLContext(sparkContext);

        // Edges group - check get correct edges
        GetDataFrameOfElements dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().edge(EDGE_GROUP).build())
                .build();
        Dataset<Row> dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }
        Set<Row> results = new HashSet<>(dataFrame.collectAsList());
        final Set<Row> expectedRows = new HashSet<>();
        final scala.collection.mutable.MutableList<Object> fields1 = new scala.collection.mutable.MutableList<>();
        scala.collection.mutable.Map<String, Long> freqMap = scala.collection.mutable.Map$.MODULE$.empty();
        freqMap.put("Y", 1000L);
        freqMap.put("Z", 10000L);
        fields1.appendElem(EDGE_GROUP);
        fields1.appendElem("B");
        fields1.appendElem("C");
        fields1.appendElem(freqMap);
        final HyperLogLogPlus hllpp = new HyperLogLogPlus(5, 5);
        hllpp.offer("AAA");
        hllpp.offer("BBB");
        fields1.appendElem(hllpp.cardinality());
        expectedRows.add(Row$.MODULE$.fromSeq(fields1));
        assertEquals(expectedRows, results);

        // Entities group - check get correct entities
        dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().entity(ENTITY_GROUP).build())
                .build();
        dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }
        results.clear();
        results.addAll(dataFrame.collectAsList());
        expectedRows.clear();
        fields1.clear();
        freqMap.clear();
        freqMap.put("W", 10L);
        freqMap.put("X", 100L);
        fields1.appendElem(ENTITY_GROUP);
        fields1.appendElem("A");
        fields1.appendElem(freqMap);
        final HyperLogLogPlus hllpp2 = new HyperLogLogPlus(5, 5);
        hllpp2.offer("AAA");
        fields1.appendElem(hllpp2.cardinality());
        expectedRows.add(Row$.MODULE$.fromSeq(fields1));
        assertEquals(expectedRows, results);

        sparkContext.stop();
    }

    @Test
    public void checkCanDealWithUserDefinedConversion() throws OperationException {
        final Graph graph1 = new Graph.Builder()
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataSchemaUserDefinedConversion.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/storeTypes.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final User user = new User();
        graph1.execute(new AddElements(getElementsForUserDefinedConversion()), user);

        final SparkConf sparkConf = new SparkConf()
                .setMaster("local")
                .setAppName("checkCanDealWithUserDefinedConversion")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrator", "gaffer.serialisation.kryo.Registrator")
                .set("spark.driver.allowMultipleContexts", "true");
        final SparkContext sparkContext = new SparkContext(sparkConf);
        final SQLContext sqlContext = new SQLContext(sparkContext);

        // Edges group - check get correct edges
        final List<Converter> converters = new ArrayList<>();
        converters.add(new MyPropertyConverter());
        GetDataFrameOfElements dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().edge(EDGE_GROUP).build())
                .converters(converters)
                .build();
        Dataset<Row> dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }
        Set<Row> results = new HashSet<>(dataFrame.collectAsList());
        final Set<Row> expectedRows = new HashSet<>();
        final scala.collection.mutable.MutableList<Object> fields1 = new scala.collection.mutable.MutableList<>();
        scala.collection.mutable.Map<String, Long> freqMap = scala.collection.mutable.Map$.MODULE$.empty();
        freqMap.put("Y", 1000L);
        freqMap.put("Z", 10000L);
        fields1.appendElem(EDGE_GROUP);
        fields1.appendElem("B");
        fields1.appendElem("C");
        fields1.appendElem(freqMap);
        final HyperLogLogPlus hllpp = new HyperLogLogPlus(5, 5);
        hllpp.offer("AAA");
        hllpp.offer("BBB");
        fields1.appendElem(hllpp.cardinality());
        fields1.appendElem(50);
        expectedRows.add(Row$.MODULE$.fromSeq(fields1));
        assertEquals(expectedRows, results);

        // Entities group - check get correct entities
        dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder().entity(ENTITY_GROUP).build())
                .converters(converters)
                .build();
        dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }
        results.clear();
        results.addAll(dataFrame.collectAsList());
        expectedRows.clear();
        fields1.clear();
        freqMap.clear();
        freqMap.put("W", 10L);
        freqMap.put("X", 100L);
        fields1.appendElem(ENTITY_GROUP);
        fields1.appendElem("A");
        fields1.appendElem(freqMap);
        final HyperLogLogPlus hllpp2 = new HyperLogLogPlus(5, 5);
        hllpp2.offer("AAA");
        fields1.appendElem(hllpp2.cardinality());
        fields1.appendElem(10);
        expectedRows.add(Row$.MODULE$.fromSeq(fields1));
        assertEquals(expectedRows, results);

        sparkContext.stop();
    }

    @Test
    public void checkViewIsRespected() throws OperationException {
        final Graph graph1 = new Graph.Builder()
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataSchema.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/dataTypes.json"))
                .addSchema(getClass().getResourceAsStream("/schema-DataFrame/storeTypes.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final User user = new User();
        graph1.execute(new AddElements(getElements()), user);

        final SparkConf sparkConf = new SparkConf()
                .setMaster("local")
                .setAppName("checkViewIsRespected")
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.kryo.registrator", "gaffer.serialisation.kryo.Registrator")
                .set("spark.driver.allowMultipleContexts", "true");
        final SparkContext sparkContext = new SparkContext(sparkConf);
        final SQLContext sqlContext = new SQLContext(sparkContext);

        // Edges group - check get correct edges
        GetDataFrameOfElements dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder()
                        .edge(EDGE_GROUP, new ViewElementDefinition.Builder()
                                .preAggregationFilter(new ElementFilter.Builder()
                                        .select("count")
                                        .execute(new IsMoreThan(800L))
                                        .build())
                                .build())
                        .build())
                .build();
        Dataset<Row> dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }
        Set<Row> results = new HashSet<>(dataFrame.collectAsList());
        final Set<Row> expectedRows = new HashSet<>();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            if (i * 200L > 800L) {
                final scala.collection.mutable.MutableList<Object> fields2 = new scala.collection.mutable.MutableList<>();
                fields2.appendElem(EDGE_GROUP);
                fields2.appendElem("" + i);
                fields2.appendElem("C");
                fields2.appendElem(6);
                fields2.appendElem(7);
                fields2.appendElem(8.0F);
                fields2.appendElem(9.0D);
                fields2.appendElem(10L);
                fields2.appendElem(i * 200L);
                expectedRows.add(Row$.MODULE$.fromSeq(fields2));
            }
        }
        assertEquals(expectedRows, results);

        // Entities group - check get correct entities
        dfOperation = new GetDataFrameOfElements.Builder()
                .sqlContext(sqlContext)
                .view(new View.Builder()
                        .entity(ENTITY_GROUP, new ViewElementDefinition.Builder()
                                .postAggregationFilter(new ElementFilter.Builder()
                                    .select("property1")
                                    .execute(new IsMoreThan(1))
                                    .build())
                            .build())
                        .build())
                .build();
        dataFrame = graph1.execute(dfOperation, user);
        if (dataFrame == null) {
            fail("No DataFrame returned");
        }
        results.clear();
        results.addAll(dataFrame.collectAsList());
        expectedRows.clear();
        for (int i = 2; i < NUM_ELEMENTS; i++) {
            final scala.collection.mutable.MutableList<Object> fields1 = new scala.collection.mutable.MutableList<>();
            fields1.clear();
            fields1.appendElem(ENTITY_GROUP);
            fields1.appendElem("" + i);
            fields1.appendElem(1);
            fields1.appendElem(i);
            fields1.appendElem(3.0F);
            fields1.appendElem(4.0D);
            fields1.appendElem(5L);
            fields1.appendElem(6);
            expectedRows.add(Row$.MODULE$.fromSeq(fields1));
        }
        assertEquals(expectedRows, results);

        sparkContext.stop();
    }

    private static List<Element> getElements() {
        final List<Element> elements = new ArrayList<>();
        for (int i = 0; i < NUM_ELEMENTS; i++) {
            final Entity entity = new Entity(ENTITY_GROUP);
            entity.setVertex("" + i);
            entity.putProperty("columnQualifier", 1);
            entity.putProperty("property1", i);
            entity.putProperty("property2", 3.0F);
            entity.putProperty("property3", 4.0D);
            entity.putProperty("property4", 5L);
            entity.putProperty("count", 6L);

            final Edge edge1 = new Edge(EDGE_GROUP);
            edge1.setSource("" + i);
            edge1.setDestination("B");
            edge1.setDirected(true);
            edge1.putProperty("columnQualifier", 1);
            edge1.putProperty("property1", 2);
            edge1.putProperty("property2", 3.0F);
            edge1.putProperty("property3", 4.0D);
            edge1.putProperty("property4", 5L);
            edge1.putProperty("count", 100L);

            final Edge edge2 = new Edge(EDGE_GROUP);
            edge2.setSource("" + i);
            edge2.setDestination("C");
            edge2.setDirected(true);
            edge2.putProperty("columnQualifier", 6);
            edge2.putProperty("property1", 7);
            edge2.putProperty("property2", 8.0F);
            edge2.putProperty("property3", 9.0D);
            edge2.putProperty("property4", 10L);
            edge2.putProperty("count", i * 200L);

            elements.add(edge1);
            elements.add(edge2);
            elements.add(entity);
        }
        return elements;
    }

    private static List<Element> getElementsWithNonStandardProperties() {
        final List<Element> elements = new ArrayList<>();
        final Entity entity = new Entity(ENTITY_GROUP);
        entity.setVertex("A");
        final FreqMap freqMap = new FreqMap();
        freqMap.put("W", 10L);
        freqMap.put("X", 100L);
        entity.putProperty("freqMap", freqMap);
        final HyperLogLogPlus hllpp = new HyperLogLogPlus(5, 5);
        hllpp.offer("AAA");
        entity.putProperty("hllpp", hllpp);
        elements.add(entity);
        final Edge edge = new Edge(EDGE_GROUP);
        edge.setSource("B");
        edge.setDestination("C");
        edge.setDirected(true);
        final FreqMap freqMap2 = new FreqMap();
        freqMap2.put("Y", 1000L);
        freqMap2.put("Z", 10000L);
        edge.putProperty("freqMap", freqMap2);
        final HyperLogLogPlus hllpp2 = new HyperLogLogPlus(5, 5);
        hllpp2.offer("AAA");
        hllpp2.offer("BBB");
        edge.putProperty("hllpp", hllpp2);
        elements.add(edge);
        return elements;
    }

    private static List<Element> getElementsForUserDefinedConversion() {
        final List<Element> elements = new ArrayList<>();
        final Entity entity = new Entity(ENTITY_GROUP);
        entity.setVertex("A");
        final FreqMap freqMap = new FreqMap();
        freqMap.put("W", 10L);
        freqMap.put("X", 100L);
        entity.putProperty("freqMap", freqMap);
        final HyperLogLogPlus hllpp = new HyperLogLogPlus(5, 5);
        hllpp.offer("AAA");
        entity.putProperty("hllpp", hllpp);
        entity.putProperty("myProperty", new MyProperty(10));
        elements.add(entity);
        final Edge edge = new Edge(EDGE_GROUP);
        edge.setSource("B");
        edge.setDestination("C");
        edge.setDirected(true);
        final FreqMap freqMap2 = new FreqMap();
        freqMap2.put("Y", 1000L);
        freqMap2.put("Z", 10000L);
        edge.putProperty("freqMap", freqMap2);
        final HyperLogLogPlus hllpp2 = new HyperLogLogPlus(5, 5);
        hllpp2.offer("AAA");
        hllpp2.offer("BBB");
        edge.putProperty("hllpp", hllpp2);
        edge.putProperty("myProperty", new MyProperty(50));
        elements.add(edge);
        return elements;
    }

    private static class MyPropertyConverter implements Converter, Serializable {
        private static final long serialVersionUID = 7777521632508320165L;

        @Override
        public boolean canHandle(Class clazz) {
            return MyProperty.class.equals(clazz);
        }

        @Override
        public DataType convertedType() {
            return DataTypes.IntegerType;
        }

        @Override
        public Object convert(Object object) throws ConversionException {
            return ((MyProperty) object).getA();
        }
    }

}