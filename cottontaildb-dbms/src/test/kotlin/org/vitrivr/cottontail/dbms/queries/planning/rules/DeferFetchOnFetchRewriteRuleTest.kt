package org.vitrivr.cottontail.dbms.queries.planning.rules

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.vitrivr.cottontail.core.database.ColumnDef
import org.vitrivr.cottontail.core.database.Name
import org.vitrivr.cottontail.core.queries.predicates.BooleanPredicate
import org.vitrivr.cottontail.core.queries.predicates.ComparisonOperator
import org.vitrivr.cottontail.core.values.types.Types
import org.vitrivr.cottontail.dbms.catalogue.CatalogueTx
import org.vitrivr.cottontail.dbms.entity.AbstractEntityTest
import org.vitrivr.cottontail.dbms.entity.EntityTx
import org.vitrivr.cottontail.dbms.execution.transactions.TransactionType
import org.vitrivr.cottontail.dbms.queries.binding.DefaultBindingContext
import org.vitrivr.cottontail.dbms.queries.context.DefaultQueryContext
import org.vitrivr.cottontail.dbms.queries.operators.logical.predicates.FilterLogicalOperatorNode
import org.vitrivr.cottontail.dbms.queries.operators.logical.projection.SelectProjectionLogicalOperatorNode
import org.vitrivr.cottontail.dbms.queries.operators.logical.sources.EntityScanLogicalOperatorNode
import org.vitrivr.cottontail.dbms.queries.operators.logical.transform.FetchLogicalOperatorNode
import org.vitrivr.cottontail.dbms.queries.planning.rules.logical.DeferFetchOnFetchRewriteRule
import org.vitrivr.cottontail.dbms.queries.planning.rules.logical.DeferFetchOnScanRewriteRule
import org.vitrivr.cottontail.dbms.schema.SchemaTx

/**
 * A collection of test cases for the [DeferFetchOnScanRewriteRule].
 *
 * @author Ralph Gasser
 * @version 1.3.0
 */
class DeferFetchOnFetchRewriteRuleTest : AbstractEntityTest() {

    /** [Name.EntityName] of test entity. */
    private val entityName = this.schemaName.entity("test-entity")

    /** [ColumnDef] in test entity. */
    private val columns = listOf(
        ColumnDef(this.entityName.column("id"), Types.Long),
        ColumnDef(this.entityName.column("doubleValue"), Types.Double),
        ColumnDef(this.entityName.column("stringValue"), Types.String),
        ColumnDef(this.entityName.column("intValue"), Types.Int),
        ColumnDef(this.entityName.column("booleanValue"), Types.Boolean)
    )

    /** List of entities that should be prepared for this test. */
    override val entities: List<Pair<Name.EntityName, List<ColumnDef<*>>>> = listOf(
        this.entityName to this.columns
    )

    /**
     * Makes a basic test whether [DeferFetchOnFetchRewriteRule.canBeApplied] works as expected and generates output accordingly.
     */
    @Test
    fun testNoMatch() {
        val txn = this.manager.TransactionImpl(TransactionType.SYSTEM)
        try {
            val ctx = DefaultQueryContext("test", this.catalogue, txn)
            val catalogueTx = txn.getTx(this.catalogue) as CatalogueTx
            val schema = catalogueTx.schemaForName(this.schemaName)
            val schemaTx = txn.getTx(schema) as SchemaTx
            val entity = schemaTx.entityForName(this.entityName)
            val entityTx = txn.getTx(entity) as EntityTx

            /* Prepare simple scan with projection. */
            val scan0 = EntityScanLogicalOperatorNode(0, entityTx, this.columns.map { ctx.bindings.bind(it) to it })
            SelectProjectionLogicalOperatorNode(scan0, this.columns.map { it.name })

            /* Check DeferFetchOnFetchRewriteRule.canBeApplied and test output for null. */
            Assertions.assertFalse(DeferFetchOnFetchRewriteRule.canBeApplied(scan0, ctx))
            Assertions.assertThrows(IllegalArgumentException::class.java) {
                DeferFetchOnFetchRewriteRule.apply(scan0, ctx)
            }
        } finally {
            txn.rollback()
        }
    }

    /**
     * Tests simple deferral and dropping of columns in a SELECT followed by a PROJECTION
     */
    @Test
    fun testDeferAfterFilter() {
        val txn = this.manager.TransactionImpl(TransactionType.SYSTEM)
        try {
            val ctx = DefaultQueryContext("test", this.catalogue, txn)
            val catalogueTx = txn.getTx(this.catalogue) as CatalogueTx
            val schema = catalogueTx.schemaForName(this.schemaName)
            val schemaTx = txn.getTx(schema) as SchemaTx
            val entity = schemaTx.entityForName(this.entityName)
            val entityTx = txn.getTx(entity) as EntityTx

            /* Prepare simple SCAN followed by a FILTER, followed by a PROJECTION. */
            val context = DefaultBindingContext()
            val scan0 = EntityScanLogicalOperatorNode(0, entityTx, this.columns.map { ctx.bindings.bind(it) to it })
            val filter0 = FilterLogicalOperatorNode(scan0, BooleanPredicate.Atomic(ComparisonOperator.Binary.Equal(context.bind(this.columns[2]), context.bindNull(this.columns[2].type)), false))
            val projection0 = SelectProjectionLogicalOperatorNode(filter0, listOf(this.columns[0].name, this.columns[1].name))

            /* Step 1: Execute DeferFetchOnScanRewriteRule and make basic assertions. */
            Assertions.assertFalse(DeferFetchOnFetchRewriteRule.canBeApplied(scan0, ctx))
            Assertions.assertTrue(DeferFetchOnScanRewriteRule.canBeApplied(scan0, ctx))
            val result1 = DeferFetchOnScanRewriteRule.apply(scan0, ctx)

            /* Check order: SCAN -> FILTER -> FETCH -> PROJECT. */
            Assertions.assertTrue(result1 is SelectProjectionLogicalOperatorNode)
            Assertions.assertTrue(result1!!.columns.equals(projection0.columns))
            val fetch1 = (result1 as SelectProjectionLogicalOperatorNode).input
            Assertions.assertTrue(fetch1 is FetchLogicalOperatorNode)
            val filter1 = (fetch1 as FetchLogicalOperatorNode).input
            Assertions.assertTrue(filter1 is FilterLogicalOperatorNode)
            val scan1 = (filter1 as FilterLogicalOperatorNode).input
            Assertions.assertTrue(scan1 is EntityScanLogicalOperatorNode)

            /* Check that columns are preserved and unnecessary columns are dropped. */
            Assertions.assertTrue(result1.columns == projection0.columns) /* Columns of the resulting PROJECTION should be the same as the original PROJECTION */

            val combined = ((scan1 as EntityScanLogicalOperatorNode).columns + fetch1.fetch) /* Columns of the SCAN + FETCH. */
            Assertions.assertEquals(scan0.columns.size, combined.size) /* Columns FETCHED + columns SCANNED should of the same size as the SCAN columns. */
            Assertions.assertTrue(scan1.columns.all { combined.contains(it) }) /* Columns FETCHED + columns SCANNED should be all columns in the original SCAN. */
            Assertions.assertTrue(scan1.columns == filter0.predicate.columns.toList()) /* Columns SCANNED should only contain the columns used by FILTER. */

            /* Step 2: Execute DeferFetchOnFetchRewriteRule and make basic assertions. */
            Assertions.assertTrue(DeferFetchOnFetchRewriteRule.canBeApplied(result1.input!!, ctx))
            val result2 = DeferFetchOnFetchRewriteRule.apply(result1.input!!, ctx)

            /* Check order: SCAN -> FILTER -> FETCH -> PROJECT. */
            Assertions.assertTrue(result2 is SelectProjectionLogicalOperatorNode)
            Assertions.assertTrue(result2!!.columns == projection0.columns)
            val fetch2 = (result2 as SelectProjectionLogicalOperatorNode).input
            Assertions.assertTrue(fetch2 is FetchLogicalOperatorNode)
            val filter2 = (fetch2 as FetchLogicalOperatorNode).input
            Assertions.assertTrue(filter2 is FilterLogicalOperatorNode)
            val scan2 = (filter2 as FilterLogicalOperatorNode).input
            Assertions.assertTrue(scan2 is EntityScanLogicalOperatorNode)

            /* Check that columns are preserved and unnecessary columns are dropped. */
            Assertions.assertTrue(result2.columns == projection0.columns) /* Columns of the resulting PROJECTION should be the same as the original PROJECTION */
            Assertions.assertTrue((scan2 as EntityScanLogicalOperatorNode).columns == filter0.predicate.columns.toList()) /* Columns SCANNED should only contain the columns used by FILTER. */
        } finally {
            txn.rollback()
        }
    }

    /**
     * Tests simple deferral and dropping of columns in a SELECT followed by a PROJECTION
     */
    @Test
    fun testRemoveUnnecessaryFetch() {
        val txn = this.manager.TransactionImpl(TransactionType.SYSTEM)
        try {
            val ctx = DefaultQueryContext("test", this.catalogue, txn)
            val catalogueTx = txn.getTx(this.catalogue) as CatalogueTx
            val schema = catalogueTx.schemaForName(this.schemaName)
            val schemaTx = txn.getTx(schema) as SchemaTx
            val entity = schemaTx.entityForName(this.entityName)
            val entityTx = txn.getTx(entity) as EntityTx
            /* Prepare simple SCAN followed by a FILTER, followed by a PROJECTION. */
            val context = DefaultBindingContext()
            val scan0 = EntityScanLogicalOperatorNode(0, entityTx, listOf(
                ctx.bindings.bind(this.columns[0]) to this.columns[0],
                ctx.bindings.bind(this.columns[1]) to this.columns[1],
                ctx.bindings.bind(this.columns[2]) to this.columns[2])
            )
            val fetch0 = FetchLogicalOperatorNode(scan0, entityTx, listOf(ctx.bindings.bind(this.columns[3]) to this.columns[3]))
            val filter0 = FilterLogicalOperatorNode(fetch0, BooleanPredicate.Atomic(ComparisonOperator.Binary.Equal(context.bind(this.columns[2]), context.bindNull(this.columns[2].type)), false))
            val projection0 = SelectProjectionLogicalOperatorNode(filter0, listOf(this.columns[0].name, this.columns[1].name))

            /* Step 1: Execute DeferFetchOnFetchRewriteRule and make basic assertions. */
            Assertions.assertTrue(DeferFetchOnFetchRewriteRule.canBeApplied(fetch0, ctx))
            Assertions.assertFalse(DeferFetchOnScanRewriteRule.canBeApplied(fetch0, ctx))
            val result1 = DeferFetchOnFetchRewriteRule.apply(fetch0, ctx)

            /* Check order: SCAN -> FILTER -> PROJECT. */
            Assertions.assertTrue(result1 is SelectProjectionLogicalOperatorNode)
            val filter1 = (result1 as SelectProjectionLogicalOperatorNode).input
            Assertions.assertTrue(filter1 is FilterLogicalOperatorNode)
            val scan1 = (filter1 as FilterLogicalOperatorNode).input
            Assertions.assertTrue(scan1 is EntityScanLogicalOperatorNode)

            /* Check that columns are preserved and unnecessary columns are dropped. */
            Assertions.assertTrue((scan1 as EntityScanLogicalOperatorNode).columns == scan0.columns)
            Assertions.assertTrue(result1.columns == projection0.columns)
        } finally {
            txn.rollback()
        }
    }

    /**
     * We don't need data for this test.
     */
    override fun populateDatabase() {
        /* No op. */
    }
}