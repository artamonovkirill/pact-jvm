package io.pactfoundation.consumer.dsl

import au.com.dius.pact.consumer.dsl.PactDslJsonArray
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue
import spock.lang.Issue
import spock.lang.Specification

import java.util.function.Consumer

class LambdaDslSpec extends Specification {

  def testArrayMinMaxLike() {
    given:
    String pactDslJson = PactDslJsonArray.arrayMinMaxLike(2, 10)
      .stringType('foo')
      .close().body

    when:
    def actualPactDsl = LambdaDsl.newJsonArrayMinMaxLike(2, 10) { o ->
      o.object { oo -> oo.stringType('foo') }
    }.build()
    String actualJson = actualPactDsl.body

    then:
    actualJson == pactDslJson
  }

  @Issue('#749')
  @SuppressWarnings('UnnecessaryObjectReferences')
  def 'newJsonArrayMinMaxLike should propagate the matchers to all items'() {
    given:
    Consumer<LambdaDslObject> snackJsonResponseFragment = { snackObject ->
      snackObject.numberType('id', 1)
      snackObject.timestamp('created', "yyyy-MM-dd'T'HH:mm:ss.SSS")
      snackObject.timestamp('lastModified', "yyyy-MM-dd'T'HH:mm:ss.SSS")
      snackObject.stringType('creator', 'Loren')
      snackObject.numberType('quantity', 5)
      snackObject.stringType('description', 'donuts')
      snackObject.object('location') { locationObject ->
        locationObject.numberType('floor', 5)
        locationObject.stringType('room', 'south kitchen')
      }
    }
    Consumer<LambdaDslJsonArray> array = { rootArray -> rootArray.object(snackJsonResponseFragment) }

    when:
    def result = LambdaDsl.newJsonArrayMinMaxLike(2, 2, array).build()
    def result2 = LambdaDsl.newJsonArrayMinLike(2, array).build()
    def result3 = LambdaDsl.newJsonArrayMaxLike(2, array).build()

    then:
    result.matchers.matchingRules.keySet() == [
      '', '[*].id', '[*].created', '[*].lastModified', '[*].creator',
      '[*].quantity', '[*].description', '[*].location.floor', '[*].location.room'
    ] as Set
    result2.matchers.matchingRules.keySet() == [
      '', '[*].id', '[*].created', '[*].lastModified', '[*].creator',
      '[*].quantity', '[*].description', '[*].location.floor', '[*].location.room'
    ] as Set
    result3.matchers.matchingRules.keySet() == [
      '', '[*].id', '[*].created', '[*].lastModified', '[*].creator',
      '[*].quantity', '[*].description', '[*].location.floor', '[*].location.room'
    ] as Set
  }

  @Issue('#778')
  def 'each key like should handle primitive values'() {
    /*
    {
      "offer": {
        "prices": {
          "DE": 1620
        },
        "shippingCosts": {
          "DE": {
            "cia": 300
          }
        }
    }
     */

    given:
    Consumer<LambdaDslObject> jsonObject = { object ->
      object.object('offer') { offer ->
        offer.object('prices') { prices ->
          prices.eachKeyLike('DE', PactDslJsonRootValue.numberType(1620))
        }
        offer.object('shippingCosts') { shippingCosts ->
          shippingCosts.eachKeyLike('DE') { cost ->
            cost.numberValue('cia', 300)
          }
        }
      }
    }

    when:
    def result = LambdaDsl.newJsonBody(jsonObject).build()

    then:
    result.matchers.matchingRules.keySet() == ['.offer.prices.*', '.offer.shippingCosts.*'] as Set
    result.toString() == '{"offer":{"prices":{"DE":1620},"shippingCosts":{"DE":{"cia":300}}}}'

  }

  @Issue('#829')
  def 'supports arrays of primitives in objects'() {
    given:
    Consumer<LambdaDslObject> object = { object ->
      object.eachLike('componentsIds', PactDslJsonRootValue.stringType('A1'))
      object.eachLike('componentsIds2', PactDslJsonRootValue.stringType('A1'), 5)
    }

    when:
    def result = LambdaDsl.newJsonBody(object).build()

    then:
    result.body.toString() == '{"componentsIds":["A1"],"componentsIds2":["A1","A1","A1","A1","A1"]}'
    result.matchers.matchingRules.keySet() == ['.componentsIds', '.componentsIds[*]', '.componentsIds2',
                                               '.componentsIds2[*]'] as Set
  }

  @Issue('#829')
  def 'supports arrays of primitives in arrays'() {
    given:
    Consumer<LambdaDslJsonArray> array = { array ->
      array.eachLike(PactDslJsonRootValue.stringType('A1'))
      array.eachLike(PactDslJsonRootValue.stringType('A1'), 5)
    }

    when:
    def result = LambdaDsl.newJsonArray(array).build()

    then:
    result.body.toString() == '[["A1"],["A1","A1","A1","A1","A1"]]'
    result.matchers.matchingRules.keySet() == ['[0]', '[0][*]', '[1]', '[1][*]'] as Set
  }

}
