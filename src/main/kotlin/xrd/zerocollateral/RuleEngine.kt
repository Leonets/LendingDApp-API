package xrd.zerocollateral

import java.util.concurrent.ConcurrentHashMap

fun interface RuleCondition<in T> : (T) -> Boolean
fun interface RuleEffect<in T, out V> : (T) -> V

class Rule<in T, out V> private constructor(
    val name: String,
    val description: String,
    private val ruleCondition: RuleCondition<T>,
    private val ruleEffect: RuleEffect<T, V>
) {

    infix fun triggersFor(input: T): Boolean {
        return ruleCondition(input)
    }

    operator fun invoke(input: T): V = ruleEffect(input)

    companion object {

        interface RuleBuilder<T, V> {

            fun description(desc: ()->String)

            fun given(block: T.() -> Boolean)

            fun then(block: T.() -> V)
        }

        private class RuleBuilderHelper<T,V>: RuleBuilder<T, V> {

            private lateinit var ruleCondition: RuleCondition<T>

            private lateinit var ruleEffect: RuleEffect<T, V>

            private lateinit var textDesc: String

            override fun description(desc: () -> String) {
                if (!this::textDesc.isInitialized) this.textDesc = desc()
                else throw Exception("Rule description has already been initialized")
            }

            override fun given(block: T.() -> Boolean) {
                if (!this::ruleCondition.isInitialized) this.ruleCondition =
                    RuleCondition {
                        block(it)
                    }
                else throw Exception("Rule condition has already been initialized")
            }

            override fun then(block: (T) -> V) {
                if (!this::ruleEffect.isInitialized) this.ruleEffect =
                    RuleEffect {
                        block(it)
                    }
                else throw Exception("Rule effect has already been initialized")
            }

            fun toRule(name: String): Rule<T, V> {
                if (!this::ruleCondition.isInitialized) throw Exception("Condition has not been defined for rule $name")
                if (!this::ruleEffect.isInitialized) throw Exception("Effect has not been defined for rule $name")
                if (!this::textDesc.isInitialized) throw Exception("Description has not been provided for rule $name")
                return Rule(name, textDesc, ruleCondition, ruleEffect)
            }

        }

        fun <T, V> rule(name: String, init: RuleBuilder<T, V>.() -> Unit): Rule<T, V> {
            val builder = RuleBuilderHelper<T, V>()
            init(builder)
            return builder.toRule(name)
        }
    }
}

class RuleSet<in T, out V> private constructor(val name: String, val description: String, private val rules: List<Rule<T, V>>, private val catchAllEffect: Effect<V>) {

    data class Effect<V>(val triggeredRule: String, val description: String, val priority: Int, val result: V)

    operator fun invoke(input: T): Effect<out V> {
        for (ruleIndex in rules.indices) {
            val theRule: Rule<T, V> = rules[ruleIndex]
            if (theRule triggersFor input) {
                return Effect(theRule.name, theRule.description, ruleIndex, theRule(input))
            }
        }
        return catchAllEffect
    }

    companion object {

        interface RuleSetBuilder<T,V> {

            fun description(descProvider: () -> String)

            fun defaultEffect(catchAll: () -> V)

            operator fun plus(rule: Rule<T, V>): RuleSetBuilder<T, V>
        }

        private class RuleSetBuilderHelper<T,V>: RuleSetBuilder<T, V> {

            private val nameRegEx = """[a-z\-]{1,50}""".toRegex()

            private lateinit var desc: String

            private lateinit var catchAllEffect: Effect<V>

            private val rules: MutableList<Rule<T, V>> = mutableListOf()

            override fun description(descProvider: () -> String) {
                if (!this::desc.isInitialized) this.desc = descProvider()
                else throw Exception("Rule description has already been initialized")
            }

            override operator fun plus(rule: Rule<T, V>): RuleSetBuilder<T, V> {
                if(rules.map { it.name }.contains(rule.name)) throw Exception("Rule ${rule.name} has already been added to rule set")
                rules.add(rule)
                return this
            }

            override fun defaultEffect(catchAll: () -> V) {
                if(!this::catchAllEffect.isInitialized) catchAllEffect = Effect("DefaultRule", "Catch-All rule only applied when no other rule has triggered", Int.MAX_VALUE, catchAll())
                else throw Exception("Catch-All effect has already been defined for this set")

            }

            fun toRuleSet(name:String): RuleSet<T, V> {
                if(!this::catchAllEffect.isInitialized) throw Exception("Default effect has not been specified for rule set $name")
                if (!this::desc.isInitialized) throw Exception("Description has not been provided for rule set $name")
                if(rules.isEmpty()) throw Exception("0 rules have been specified for rule set $name")
                if(!nameRegEx.matches(name)) throw Exception("Rule set name '$name' does not match expression ${nameRegEx.pattern}")
                return RuleSet(name, desc, rules.toList(), catchAllEffect)
            }
        }

        private fun <T,V> RuleSet<T, V>.toRegistryEntry(): RuleSetsRegistry.RuleSetEntry =
            RuleSetsRegistry.RuleSetEntry(this.name, this.description)

        fun <T, V> create(name: String, init: RuleSetBuilder<T, V>.()->Unit): RuleSet<T, V> {
            val ruleSetBuilder = RuleSetBuilderHelper<T,V>()
            init(ruleSetBuilder)
            val ruleSet = ruleSetBuilder.toRuleSet(name)
            RuleSetsRegistry.register(ruleSet.toRegistryEntry())
            return ruleSet
        }
    }
}

object RuleSetsRegistry {

    data class RuleSetEntry(val id: String, val description: String)

    private val registredRuleSets = ConcurrentHashMap<String, RuleSetEntry>()
    
    fun register(ruleSetName: RuleSetEntry) {
        registredRuleSets[ruleSetName.id] = ruleSetName
    }

    fun registeredSets(): List<RuleSetEntry> = registredRuleSets.values.toList()

}
