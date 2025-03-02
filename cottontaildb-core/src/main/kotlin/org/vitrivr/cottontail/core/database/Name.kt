package org.vitrivr.cottontail.core.database

/**
 * A [Name] that identifies a DBO used within Cottontail DB.
 *
 * @author Ralph Gasser
 * @version 1.3.0
 */
sealed interface Name: Comparable<Name> {

    companion object {
        /* Delimiter between name components. */
        const val DELIMITER = "."

        /* Character used for wildcards. */
        const val WILDCARD = "*"

        /* Root name component in Cottontail DB names. */
        const val ROOT = "warren"
    }

    /** Returns the [RootName] reference. */
    val root: RootName
        get() = RootName

    /** Returns the last component of this [Name], i.e. the simple name. */
    val fqn: String

    /** Returns the last component of this [Name], i.e. the simple name. */
    val simple: String

    /** Returns true if this [Name] matches the other [Name]. */
    fun matches(other: Name): Boolean

    /**
     * Compares this [Name] to the other [Name] by comparing their fully qualified name.
     *
     * @param other The other [Name] to compare this [Name] to.
     * @return -1, 0 or 1 If the other name is smaller, equal or greater than this name.
     */
    override fun compareTo(other: Name): Int = this.fqn.compareTo(other.fqn)

    /**
     * The [RootName] which always is 'warren'.
     */
    object RootName : Name {
        override val fqn: String = ROOT
        override val simple: String = ROOT
        fun schema(name: String) = SchemaName.create(name)
        override fun toString(): String = ROOT
        override fun matches(other: Name): Boolean = (other == this)
    }

    /**
     * A [Name] object used to identify a function in Cottontail DB.
     */
    data class FunctionName private constructor(val function: String): Name {

        companion object {
            fun create(name: String) = FunctionName(name.lowercase())
        }

        /** The fully qualified name of this [Name.FunctionName]. */
        override val fqn: String
            get() = "${ROOT}${DELIMITER}${this.function}"

        /** The simple name of this [Name.FunctionName]. */
        override val simple: String
            get() = this.function

        /**
         * Checks for a match with another [Name]. Only exact matches, i.e. equality, are possible for [SchemaName]s.
         *
         * @param other [Name] to compare to.
         * @return True on match, false otherwise.
         */
        override fun matches(other: Name): Boolean = (other == this)

        override fun toString(): String = this.fqn
    }

    /**
     * A [Name] object used to identify a schema in Cottontail DB.
     */
    data class SchemaName private constructor(val schema: String): Name {

        companion object {
            fun create(name: String) = SchemaName(name.lowercase())
        }

        init {
            require(!this.schema.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.schema.contains(WILDCARD)) { "Name component cannot contain ${WILDCARD}."}
        }

        /** The fully qualified name of this [Name.SchemaName]. */
        override val fqn: String
            get() = "${ROOT}${DELIMITER}${this.schema}"

        /** The simple name of this [Name.SchemaName]. */
        override val simple: String
            get() = this.schema

        /**
         * Generates an [EntityName] as child of this [SchemaName].
         *
         * @param name Name of the [EntityName]
         * @return [EntityName]
         */
        fun entity(name: String) = EntityName.create(this.schema, name)

        /**
         * Checks for a match with another [Name]. Only exact matches, i.e. equality, are possible for [SchemaName]s.
         *
         * @param other [Name] to compare to.
         * @return True on match, false otherwise.
         */
        override fun matches(other: Name): Boolean = (other == this)

        override fun toString(): String = this.fqn
    }

    /**
     * A [Name] object used to identify an entity in Cottontail DB.
     */
    data class EntityName private constructor(val schema: String, val entity: String): Name {
        companion object {
            fun create(schema: String, entity: String) = EntityName(schema.lowercase(), entity.lowercase())
        }

        init {
            require(!this.schema.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.schema.contains(WILDCARD)) { "Name component cannot contain ${WILDCARD}."}
            require(!this.entity.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.entity.contains(WILDCARD)) { "Name component cannot contain ${WILDCARD}."}
        }

        /** The fully qualified name of this [Name.EntityName]. */
        override val fqn: String
            get() = "${ROOT}${DELIMITER}${this.schema}${DELIMITER}${this.entity}"

        /** The fully qualified name of this [Name.EntityName]. */
        override val simple: String
            get() = this.entity

        /**
         * Returns parent [SchemaName] for this [EntityName].
         *
         * @return Parent [SchemaName]
         */
        fun schema(): SchemaName = SchemaName.create(this.schema)

        /**
         * Generates an [IndexName] as child of this [EntityName].
         *
         * @param name Name of the [IndexName]
         * @return [IndexName]
         */
        fun index(name: String) = IndexName.create(this.schema, this.entity, name)

        /**
         * Generates an [ColumnName] as child of this [EntityName].
         *
         * @param name Name of the [ColumnName]
         * @return [ColumnName]
         */
        fun column(name: String) = ColumnName.create(this.schema, this.entity, name)

        /**
         * Generates the special '__tid' [SequenceName] as child of this [EntityName].
         *
         * @return [SequenceName]
         */
        fun tid() = SequenceName.create(this.schema, this.entity, "__tid")

        /**
         * Generates an [SequenceName] as child of this [EntityName].
         *
         * @param name Name of the [SequenceName]
         * @return [SequenceName]
         */
        fun sequence(name: String): SequenceName {
            require(name != "__tid") { "The name '__tid' is reserved and cannot be used as sequence name!" }
            return SequenceName.create(this.schema, this.entity, name)
        }

        /**
         * Checks for a match with another name. Only exact matches, i.e. equality, are possible for [EntityName]s.
         *
         * @param other [Name] to compare to.
         * @return True on match, false otherwise.
         */
        override fun matches(other: Name): Boolean = (other == this)

        override fun toString(): String = this.fqn
    }

    /**
     * A [Name] object used to identify a sequence.
     */
    data class SequenceName private constructor(val schema: String, val entity: String, val sequence: String) : Name {

        companion object {
            fun create(schema: String, entity: String, sequence: String) = SequenceName(schema.lowercase(), entity.lowercase(), sequence.lowercase())
        }

        init {
            require(!this.schema.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.schema.contains(WILDCARD)) { "Name component cannot contain ${WILDCARD}."}
            require(!this.entity.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.entity.contains(WILDCARD)) { "Name component cannot contain ${WILDCARD}."}
            require(!this.sequence.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.sequence.contains(WILDCARD)) { "Name component cannot contain ${WILDCARD}."}
        }

        /** The fully qualified name of this [Name.SequenceName]. */
        override val fqn: String
            get() = "${ROOT}${DELIMITER}${this.schema}${DELIMITER}${this.entity}${DELIMITER}${this.sequence}"

        /** The fully qualified name of this [Name.SequenceName]. */
        override val simple: String
            get() = this.sequence

        /**
         * Returns parent [SchemaName] of this [SequenceName].
         *
         * @return Parent [SchemaName]
         */
        fun schema(): SchemaName = SchemaName.create(this.schema)

        /**
         * Returns parent [EntityName] of this [SequenceName].
         *
         * @return Parent [EntityName]
         */
        fun entity(): EntityName = EntityName.create(this.schema, this.entity)

        /**
         * Checks for a match with another name. Only exact matches, i.e. equality, are possible for [SequenceName]s.
         *
         * @param other [Name] to compare to.
         * @return True on match, false otherwise.
         */
        override fun matches(other: Name): Boolean = (other == this)

        override fun toString(): String = this.fqn
    }

    /**
     * A [Name] object used to identify an index in Cottontail DB
     */
    data class IndexName private constructor(val schema: String, val entity: String, val index: String) : Name {

        companion object {
            fun create(schema: String, entity: String, index: String) = IndexName(schema.lowercase(), entity.lowercase(), index.lowercase())
        }

        init {
            require(!this.schema.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.schema.contains(WILDCARD)) { "Name component cannot contain ${WILDCARD}."}
            require(!this.entity.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.entity.contains(WILDCARD)) { "Name component cannot contain ${WILDCARD}."}
            require(!this.index.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.index.contains(WILDCARD)) { "Name component cannot contain ${WILDCARD}."}
        }

        /** The fully qualified name of this [Name.IndexName]. */
        override val fqn: String
            get() = "${ROOT}${DELIMITER}${this.schema}${DELIMITER}${this.entity}${DELIMITER}${this.index}"

        /** The fully qualified name of this [Name.IndexName]. */
        override val simple: String
            get() = this.index

        /**
         * Returns parent [SchemaName] of this [IndexName].
         *
         * @return Parent [SchemaName]
         */
        fun schema(): SchemaName = SchemaName.create(this.schema)

        /**
         * Returns parent  [EntityName] of this [IndexName].
         *
         * @return Parent [EntityName]
         */
        fun entity(): EntityName = EntityName.create(this.schema, this.entity)

        /**
         * Checks for a match with another name. Only exact matches, i.e. equality, are possible for [IndexName]s.
         *
         * @param other [Name] to compare to.
         * @return True on match, false otherwise.
         */
        override fun matches(other: Name): Boolean = (other == this)

        override fun toString(): String = this.fqn
    }

    /**
     * A [Name] object used to identify an column in Cottontail DB
     */
    data class ColumnName private constructor(val schema: String, val entity: String, val column: String): Name {

        companion object {
            fun create(schema: String, entity: String, column: String) = ColumnName(schema.lowercase(), entity.lowercase(), column.lowercase())
        }

        init {
            require(!this.schema.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.entity.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
            require(!this.column.contains(DELIMITER)) { "Name component cannot contain ${DELIMITER}."}
        }

        /** Constructor for simple names. */
        constructor(columnName: String): this(WILDCARD, WILDCARD, columnName.lowercase())

        /** The fully qualified name of this [Name.IndexName]. */
        override val fqn: String
            get() = "${ROOT}${DELIMITER}${this.schema}${DELIMITER}${this.entity}${DELIMITER}${this.column}"

        /** The fully qualified name of this [Name.IndexName]. */
        override val simple: String
            get() = this.column

        /** True if this [ColumnName] is a wildcard. */
        val wildcard: Boolean
            get() = this.schema == WILDCARD || this.entity == WILDCARD || this.column == WILDCARD

        /**
         * Returns parent [SchemaName] of this [ColumnName].
         *
         * @return Parent [SchemaName]
         */
        fun schema(): SchemaName? {
            if (this.schema == WILDCARD) return null
            return SchemaName.create(this.schema)
        }

        /**
         * Returns parent [EntityName] of this [ColumnName].
         *
         * @return Parent [EntityName]
         */
        fun entity(): EntityName? {
            if (this.schema == WILDCARD || this.entity == WILDCARD) return null
            return EntityName.create(this.schema, this.entity)
        }

        /**
         * Checks for a match with another name. Wildcard matches  are possible for [ColumnName]s.
         *
         * @param other [Name] to compare to.
         * @return True on match, false otherwise.
         */
        override fun matches(other: Name): Boolean {
            if (other !is ColumnName) return false
            if (!this.wildcard) return (this == other)
            if (this.schema != WILDCARD && this.schema != other.schema) return false
            if (this.entity != WILDCARD && this.entity != other.entity) return false
            if (this.column != WILDCARD && this.column != other.column) return false
            return true
        }

        override fun toString(): String = if (this.schema == WILDCARD && this.schema == WILDCARD) {
            this.simple
        } else {
            this.fqn
        }
    }
}