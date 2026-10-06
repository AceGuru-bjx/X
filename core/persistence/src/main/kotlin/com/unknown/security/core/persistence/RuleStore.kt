package com.unknown.security.core.persistence

import android.content.Context
import com.unknown.security.core.model.DetectionRule
import com.unknown.security.core.model.RuleSet
import com.unknown.security.core.model.RuleSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Rule storage:
 * - built-in seed rules ship in assets and are always loaded;
 * - user rules live in a private JSON file and are merged on top (same id = override);
 * - the merged set is exported/imported as a single JSON document.
 */
class RuleStore(
    private val context: Context,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }

    private val mutex = Mutex()

    private val _ruleSet = MutableStateFlow(RuleSet.EMPTY)
    val ruleSet: StateFlow<RuleSet> = _ruleSet.asStateFlow()

    /** Loads seed + user rules and publishes the merged set. */
    suspend fun reload() =
        mutex.withLock {
            withContext(Dispatchers.IO) { _ruleSet.value = loadMerged() }
        }

    private fun loadMerged(): RuleSet {
        val seed = loadSeed()
        val user = loadUser()
        return RuleSet(
            rules =
                seed.rules +
                    user.rules.filterNot { userRule ->
                        seed.rules.any { it.id == userRule.id }
                    },
            formatVersion = maxOf(seed.formatVersion, user.formatVersion),
            generatedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun loadSeed(): RuleSet =
        runCatching {
            context.assets.open(SEED_ASSET).bufferedReader().use { reader ->
                json.decodeFromString(RuleSet.serializer(), reader.readText())
            }
        }.getOrDefault(RuleSet.EMPTY)

    private fun loadUser(): RuleSet {
        val file = context.filesDir.resolve(USER_FILE)
        if (!file.exists()) return RuleSet.EMPTY
        return runCatching {
            json.decodeFromString(RuleSet.serializer(), file.readText())
        }.getOrDefault(RuleSet.EMPTY)
    }

    /** Adds or replaces a user rule and persists it. */
    suspend fun upsertUserRule(rule: DetectionRule) {
        writeUserRules { rules ->
            rules.removeAll { it.id == rule.id }
            rules.add(rule)
        }
    }

    /** Removes a rule; built-in rules are disabled via a user override instead of deletion. */
    suspend fun removeRule(ruleId: String) {
        writeUserRules { rules -> rules.removeAll { it.id == ruleId } }
    }

    /** Enables or disables any rule by id; built-ins get a user override copy. */
    suspend fun toggleRule(
        ruleId: String,
        enabled: Boolean,
    ) {
        val target = _ruleSet.value.rules.firstOrNull { it.id == ruleId } ?: return
        val toggled = withRuleEnabled(target, enabled).asSource(RuleSource.USER)
        upsertUserRule(toggled)
    }

    private suspend fun writeUserRules(block: (MutableList<DetectionRule>) -> Unit) =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val rules = loadUser().rules.toMutableList()
                block(rules)
                val updated = RuleSet(rules = rules, formatVersion = 1, generatedAtMillis = System.currentTimeMillis())
                context.filesDir.resolve(USER_FILE).writeText(json.encodeToString(RuleSet.serializer(), updated))
                _ruleSet.value = loadMerged()
            }
            Unit
        }

    /** Full merged set as pretty JSON — for backup and sharing. */
    fun exportJson(): String = json.encodeToString(RuleSet.serializer(), _ruleSet.value)

    /**
     * Imports rules from a JSON document; user rules are replaced, seeds stay untouched.
     * Returns the number of imported rules.
     */
    suspend fun importJson(text: String): Result<Int> {
        val parsed = runCatching { json.decodeFromString(RuleSet.serializer(), text) }
        return parsed.mapCatching { imported ->
            val userRules = imported.rules.map { rule -> rule.asSource(RuleSource.USER) }
            writeUserRules { rules ->
                rules.clear()
                rules.addAll(userRules)
            }
            userRules.size
        }
    }

    private fun withRuleEnabled(
        rule: DetectionRule,
        enabled: Boolean,
    ): DetectionRule =
        when (rule) {
            is DetectionRule.PackageNameRule -> rule.copy(enabled = enabled)
            is DetectionRule.LabelKeywordRule -> rule.copy(enabled = enabled)
            is DetectionRule.PermissionComboRule -> rule.copy(enabled = enabled)
            is DetectionRule.ApkHashRule -> rule.copy(enabled = enabled)
            is DetectionRule.TargetSdkRule -> rule.copy(enabled = enabled)
        }

    private fun DetectionRule.asSource(source: RuleSource): DetectionRule =
        when (this) {
            is DetectionRule.PackageNameRule -> copy(source = source)
            is DetectionRule.LabelKeywordRule -> copy(source = source)
            is DetectionRule.PermissionComboRule -> copy(source = source)
            is DetectionRule.ApkHashRule -> copy(source = source)
            is DetectionRule.TargetSdkRule -> copy(source = source)
        }

    private companion object {
        const val SEED_ASSET = "rules/seed_rules.json"
        const val USER_FILE = "user_rules.json"
    }
}
