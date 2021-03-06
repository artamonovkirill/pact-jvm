package au.com.dius.pact.provider.junit

import au.com.dius.pact.model.FilteredPact
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Pact
import au.com.dius.pact.provider.junit.loader.PactFilter
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.function.Predicate

object JUnitProviderTestSupport {
  fun <I> filterPactsByAnnotations(pacts: List<Pact<I>>, testClass: Class<*>): List<Pact<I>> where I : Interaction {
    val pactFilterValues = testClass.getAnnotation(PactFilter::class.java)?.value
    return if (pactFilterValues != null && pactFilterValues.any { !it.isEmpty() }) {
      pacts.map { pact ->
        FilteredPact(pact, Predicate { interaction ->
          pactFilterValues.any { value -> interaction.providerStates.any { it.matches(value) } }
        })
      }.filter { pact -> pact.interactions.isNotEmpty() }
    } else pacts
  }

  @JvmStatic
  fun generateErrorStringFromMismatches(mismatches: Map<String, Any>): String {
    return System.lineSeparator() + mismatches.values
      .mapIndexed { i, value ->
        val errPrefix = "$i - "
        when (value) {
          is Throwable -> errPrefix + exceptionMessage(value, errPrefix.length)
          is Map<*, *> -> errPrefix + convertMapToErrorString(value as Map<String, *>)
          else -> errPrefix + value.toString()
        }
      }.joinToString(System.lineSeparator())
  }

  @JvmStatic
  fun exceptionMessage(err: Throwable, prefixLength: Int): String {
    val message = err.message

    val cause = err.cause
    var details = ""
    if (cause != null) {
      details = ExceptionUtils.getStackTrace(cause)
    }

    val lineSeparator = System.lineSeparator()
    return if (message != null && message.contains("\n")) {
      val padString = StringUtils.leftPad("", prefixLength)
      val lines = message.split("\n")
      lines.reduceIndexed { index, acc, line ->
        if (index > 0) {
          acc + lineSeparator + padString + line
        } else {
          line + lineSeparator
        }
      }
    } else {
      "$message\n$details"
    }
  }

  private fun convertMapToErrorString(mismatches: Map<String, *>): String {
    return if (mismatches.containsKey("comparison")) {
      val comparison = mismatches["comparison"]
      if (mismatches.containsKey("diff")) {
        mapToString(comparison as Map<String, *>)
      } else {
        if (comparison is Map<*, *>) {
          mapToString(comparison as Map<String, *>)
        } else {
          comparison.toString()
        }
      }
    } else {
      mapToString(mismatches)
    }
  }

  private fun mapToString(comparison: Map<String, *>): String {
    return comparison.entries.joinToString(System.lineSeparator()) { (key, value) -> "$key -> $value" }
  }
}
