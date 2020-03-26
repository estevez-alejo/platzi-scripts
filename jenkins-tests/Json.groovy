import groovy.json.JsonSlurper

def getJson() {

  def props = readJSON text: '{ "key": null, "a": "b" }', returnPojo: true
  assert props['key'] == null
  props.each { key, value ->
      echo "Walked through key $key and value $value"
  }

  return props

  /*
  def json = new JsonSlurper().parseText(text)
  def bodyText = json.body

  return bodyText*/
}

return this
