//import com.caixabank.movilidad.log.Logger
//import com.caixabank.movilidad.utils.GeneratorUtils
import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic

import java.time.*
import java.text.SimpleDateFormat
import java.util.logging.Logger
import groovy.json.JsonOutput

//import com.caixabank.movilidad.log.*



private static Map getInputMap() {
    //def inputFile = new File("/var/lib/jenkins/workspace/test-pipeline-sum-node/jenkins-tests/input.json")
    //def inputFile = new File("www.alejoestevez/input.json")
    //def inputJSON = new JsonSlurper().parseText(inputFile.text)
    def inputJSON = new JsonSlurper().parse("http://www.alejoestevez.com/input.json".toURL())
    inputJSON
}


private static Map getConfigMap() {
    def configJSON = new JsonSlurper().parse("http://www.alejoestevez.com/config.json".toURL())
    configJSON
}


def jsonInput = JsonOutput.toJson(getInputMap())
println ("Init Process JSON: ${LocalDateTime.now().toString()}")
//println ("Input Map:" + getInputMap().toString())
println JsonOutput.prettyPrint(jsonInput)



def paramsMapAND = getParamsToPipeAndApp()
//println ("Output Map:" + paramsMapAND.toString())

//def json = new groovy.json.JsonBuilder()
//json rootKey: paramsMapAND

def jsonConfig = JsonOutput.toJson(getConfigMap())
println ("Config Json Process JSON: ${LocalDateTime.now().toString()}")
println JsonOutput.prettyPrint(jsonConfig)



def jsonOutput = JsonOutput.toJson(paramsMapAND)
println ("EndProcess Json Process JSON: ${LocalDateTime.now().toString()}")
println JsonOutput.prettyPrint(jsonOutput)





private static Map getParamsToPipeAndApp() {
    //Logger.info(context, "Into AppGeneratorAndGsa.getParamsToPipeAndApp", levelOfLog)
    def paramsMapAND = [:]
    def paramsMapANDTrazabilidad = new ArrayList<HashMap>(1)
    def paramsMapANDTrazabilidadHash = [:]
    def paramsMapANDParametros = new ArrayList<HashMap>()
    def paramsMapANDDepsComp = new ArrayList<HashMap>(1)

    //def inputFile = new File("/var/lib/jenkins/workspace/test-pipeline-sum-node/jenkins-tests/input.json")

    //def parametersMap = new JsonSlurper().parseText(inputFile.text)
    def parametersMap = getInputMap()


    def type = "IOS.BF"
    Date date = new Date()
    SimpleDateFormat timestampFormatDay = new SimpleDateFormat("yyyyMMdd");


    def timestamp = String.valueOf(timestampFormatDay.format(date))

    if (type != null) {
        paramsMapAND.put("tipo", type)
    }

    if (parametersMap.garName != null) {
        paramsMapAND.put("nombre", parametersMap.garName)
        //Logger.info(context,"Into AppGeneratorAndGsa.getParamsToPipeAndApp name "+context.CI_PARAMS.getGARName(),levelOfLog)
    }


    if (parametersMap.version != null) {

        if (parametersMap.BUILD_CODE != null && parametersMap.BUILD_CODE != 0) {
            paramsMapAND.put("version", parametersMap.version + "-" + parametersMap.BUILD_CODE)
            //add buildcode for test. For release version drop
            //Logger.info(context,"Into AppGeneratorAndGsa.getParamsToPipeAndApp version" + context.CI_PARAMS.getVersion() +"-"+ context.BUILD_CODE,levelOfLog)
        } else {
            paramsMapAND.put("version", parametersMap.version) //add buildcode for test. For release version drop
            //Logger.info(context,"Into AppGeneratorAndGsa.getParamsToPipeAndApp version " + context.CI_PARAMS.getVersion(),levelOfLog)
        }

    }

    if (parametersMap.buildVariant != null) {
        paramsMapAND.put("variant", parametersMap.buildVariant)
        //Logger.info(context,"Into AppGeneratorAndGsa.getParamsToPipeAndApp variant "+context.CI_PARAMS.getBuildVariant(),levelOfLog)
    }

    if (parametersMap.REPOSITORY_URL != null) {
        paramsMapAND.put("sourceCode", parametersMap.REPOSITORY_URL)
        //Logger.info(context,"Into AppGeneratorAndGsa.getParamsToPipeAndApp sourceCode "+context.REPOSITORY_URL,levelOfLog)
    }

    if (parametersMap.nexusUrl != null) {

        paramsMapAND.put("binarySource", parametersMap.nexusUrl)
        //Logger.info(context,"Into AppGeneratorAndGsa.getParamsToPipeAndApp binarySource "+ context.NEXUS_REPOSITORY+context.CI_PARAMS.getGroupId().replace('.', '/')+"/"+
        //		context.CI_PARAMS.getArtifactId()+"/"+context.CI_PARAMS.getVersion(),levelOfLog)
    }


    if (parametersMap.isSnapshot) {
        paramsMapANDTrazabilidadHash.put("type", "SNAP")
    } else if (parametersMap.isRelease) {
        paramsMapANDTrazabilidadHash.put("type", "REL")
    } else if (parametersMap.isStore) {
        paramsMapANDTrazabilidadHash.put("type", "STORE")
    } else {
        paramsMapANDTrazabilidadHash.put("type", "null")
        //Logger.warn(context, "Don't find type value of Trazabilidad", levelOfLog)
    }

    paramsMapANDTrazabilidadHash.put("tsNexus", timestamp)

    paramsMapANDTrazabilidad.add(paramsMapANDTrazabilidadHash)

    paramsMapAND.put("trazabilidad", paramsMapANDTrazabilidad)


//Logger.info(context,paramsMapAND.toString(),levelOfLog)

    paramsMapANDParametros = this.getParametrosFromAndApp(parametersMap, "INFO", type)


    paramsMapANDDepsComp = this.getDepsComponentesAndThird(parametersMap, "INFO", type, paramsMapANDParametros)

    if (paramsMapANDDepsComp != null) {

        paramsMapAND.put("depsComponentess", paramsMapANDDepsComp)
    }

    if (paramsMapANDParametros != null) {

        //reduce array to not null because I need not null values
        paramsMapANDParametros.trimToSize()

        if (paramsMapANDParametros.size() > 0) {
            paramsMapAND.put("parametros", paramsMapANDParametros)
        }
    }

//    Logger.info(context, "Finish AppGeneratorAndGsa.getParamsToPipeAnd", levelOfLog)
    return paramsMapAND

}


public static ArrayList<HashMap> getDepsComponentesAndThird(parametersMap,levelOfLog, type, paramsMapParametros){

  //  Logger.info(context,"Into GeneratorUtils.getDepsComponentesAndThird",levelOfLog)

    def paramsDepsCompArray = new ArrayList<HashMap>()
    def thirdPartyCount = 0
    def hashDepArray = new ArrayList<HashMap>()
    def hashDepThird = [:]
    def technologyComp = type.substring(0,3)
    def podsDepency = new ArrayList<StringBuilder>()

    for(int i = 0; i < parametersMap.firstLevelDependencies.size(); i++){
        def hashDep = [:]
        def hashDep2 = [:]

        //android Case ( app or Aar )
        if("AND".equalsIgnoreCase(technologyComp)) {
           // Logger.info(context,"getDepsComponentes From And ->> "+parametersMap.firstLevelDependencies[i],levelOfLog)

            def tokDep = parametersMap.firstLevelDependencies[i].tokenize(':')
            def tipo =tokDep[0].tokenize('\\.')

            if ("ADAM".equalsIgnoreCase(tipo[0].toUpperCase())) {
                hashDep.put("tipo", (tipo[1].substring(0,3)+"."+tipo[2]).toUpperCase())
                hashDep.put("nombre", tokDep[1])

                def prepare = (tokDep[2].indexOf("->") > 0)
                hashDep.put("version", prepare ? tokDep[2].substring(tokDep[2].indexOf("->") + 2 ).trim() : tokDep[2])
                paramsDepsCompArray.add(hashDep)
            }else {
                //Third Party dependencies
                hashDep.put("propiedad", "librerias."+thirdPartyCount+".nombre")
                hashDep.put("valor", tokDep[0])
                hashDepArray.add( hashDep )
                hashDep2.put("propiedad", "librerias."+thirdPartyCount+".version")

                def prepare = (tokDep[2].indexOf("->") > 0)
                hashDep2.put("valor", prepare ? tokDep[2].substring(tokDep[2].indexOf("->") + 2 ).trim() : tokDep[2])

                hashDepArray.add( hashDep2 )
                thirdPartyCount++
            }
        }else {
           // Logger.error(context,"Don't parse technology at getDepsComponentes",levelOfLog)
        }
    }

    //Drop duplicate elements

    Set<HashMap> mySet = new LinkedHashSet<HashMap>(paramsDepsCompArray)
    paramsDepsCompArray.clear()
    paramsDepsCompArray.addAll(mySet)

    if (thirdPartyCount > 0 ) {
        //Name has refactored to Oriol GSA to thirdparties
        hashDepThird.put("fichero", "thirdparties")
        hashDepThird.put("contenido", hashDepArray)
        paramsMapParametros.add( hashDepThird )
    }


  //  Logger.info(context,"Out of  GeneratorUtils.getDepsComponentesAndThird",levelOfLog)

    return paramsDepsCompArray
}


public static ArrayList<HashMap> getParametrosFromAndApp(parametersMap,levelOfLog, technology){

   // Logger.info(context,"Into AppGeneratorAndGsa.getParametrosFromAndApp",levelOfLog)

    def pathConfigMethods
    String methodsString = ""
    String configString = ""
    def paramsParametrosArray = new ArrayList<HashMap>()
    def methodsOk = false
    def configOk = false

    paramsParametrosArray = this.getParametrosConfigMethodsFromFiles(levelOfLog, technology, pathConfigMethods, pathConfigMethods )

    //Logger.info(context,"Out of  AppGeneratorAndGsa.getParametrosFromAndApp",levelOfLog)
    return paramsParametrosArray
}

private static String findPathServicesAnd(context,parametersMap,levelOfLog) {

    String pathServices = ""
    def dest

    if(parametersMap.codeFolder  != null ){



        dest = "${parametersMap.repoName}/src/${parametersMap.buildVariant}/assets/json/"


        pathServices = parametersMap.codeFolder  + "/"+dest

    }else {
        //Logger.error(context,"Dont' find Gar Name of repo at findPathServices", levelOfLog)
    }

    return pathServices
}

private static Map parseFileToParametrosMap(levelOfLog, jsonFileText) {

    def contenidoMap = new ArrayList<HashMap>()
    println("call  parseFileToParametrosMap")
    if(jsonFileText != null ) {

        try {

         //   Logger.info(context,"[INFO] Into parseFileToParametrosMap",levelOfLog)


            def startTime = System.currentTimeMillis()

          //  def JsonSlurperClassic = new JsonSlurperClassic()
            //def messagesJson = JsonSlurperClassic.parseText(jsonFileText.text)
            //def messagesJson = JsonSlurperClassic.parseText(jsonFileText)
def messagesJson = jsonFileText

            println("testing " + messagesJson)
            //Go across the json file and create  propiedad of path of structure json and value
            //context.println("Json parser is "+messagesJson+" and is the type "+messagesJson.getClass())

            //create Array of new Class about array and map of propiedad and valor only
            createContenido(levelOfLog, messagesJson ,"", contenidoMap )

            def timeDiff = System.currentTimeMillis() - startTime

           // Logger.info(context,"parseFileToParametrosMap took ${timeDiff} ms",levelOfLog)


        }catch(Exception e) {
            println("error")
         //   Logger.warn(context,"Don't parse "+ jsonFileText+ " of Json inside  GeneratorUtils.parseFileToParametrosMap ",levelOfLog)
        }

    }else {
        println("error")
       // Logger.warn(context,"Don't find file "+ jsonFileText+ " of Json inside  GeneratorUtils.parseFileToParametrosMap ",levelOfLog)
    }

    //return contenidoMap.columns.collectEntries{[it.propiedad, it.valor]}

    return contenidoMap.collectEntries { it -> [it.propiedad, it.valor]}
}

private static void createContenido(levelOfLog, messagesJson , keyPath, contenidoMap ) {

   // Logger.info(context,"[INFO] Into GeneratorUtils.createContenido",levelOfLog)

    def startTime = System.currentTimeMillis()

    //messagesJson.each{ k, v ->
    for (Map.Entry<String,Object> entry : messagesJson.entrySet()) {
        Object v = entry.getValue()
        String k = entry.getKey()
       // Logger.debug(context,"GeneratorUtils.createContenido on "+keyPath + " value "+ v + " is the Class "+v.getClass(),levelOfLog)

        this.addCreateContenido(levelOfLog, k,v , keyPath, contenidoMap )

    }

    def timeDiff = System.currentTimeMillis() - startTime
   // Logger.info(context,"GeneratorUtils.createContenido took ${timeDiff} ms",levelOfLog)
}

private static void addCreateContenido(levelOfLog, k, v , keyPath, contenidoMap ) {

   // Logger.info(context,"[INFO] Into GeneratorUtils.addCreateContenido",levelOfLog)

    def startTime = System.currentTimeMillis()

    if (v instanceof String || v instanceof Integer) {

        def tempHash = [:]
     //   Logger.debug(context,"GeneratorUtils.createContenido put "+keyPath + " value "+v,levelOfLog)
        if (!"".equalsIgnoreCase(keyPath)) {
            tempHash.put("propiedad", keyPath)
        }else {
            tempHash.put("propiedad", k)
        }
        tempHash.put("valor", "" + v)  //Cast to String if is Integer
        contenidoMap.add(tempHash)

    }else if ( v instanceof HashMap) {
        //Logger.info(context,"GeneratorUtils.createContenido pass HashMap "+keyPath + "." + k + " value "+v,levelOfLog)
        String keyPathMap = keyPath
        String keyTemp = ""
        //Logger.info(context,"GeneratorUtils.createContenido class "+ v.getClass()+" value "+v,levelOfLog)
        for (Map.Entry<String,Object> entryHashMap : v.entrySet()) {

            Object vHasMap = entryHashMap.getValue()
            String  kHashMap = entryHashMap.getKey()
            if (!"".equalsIgnoreCase(keyPath)) {
                keyTemp = keyPath +"."+ kHashMap

            }else {
                keyTemp = k +"."+ kHashMap
            }

            this.addCreateContenido(levelOfLog, kHashMap, vHasMap , keyTemp, contenidoMap)
            keyPath = keyPathMap
        }
    }else if ( v instanceof ArrayList) {
        int arrayInt = 0
        //Logger.info(context,"GeneratorUtils.createContenido pass Array"+keyPath + " value "+v,levelOfLog)
        String keyPathArray = keyPath
        String keyTemp = ""
        for (Object entryArray : v) {

            if (!"".equalsIgnoreCase(keyPath)) {
                keyTemp = keyPath + "." + arrayInt++
            }else {
                keyTemp = k + "." + arrayInt++
            }

            this.addCreateContenido(levelOfLog, "", entryArray , keyTemp , contenidoMap)
            keyPath = keyPathArray
        }

    }else {
      //  Logger.error(context,"Dont' find appropiate class for "+v.getClass() + " at node "+k, levelOfLog)
    }

    def timeDiff = System.currentTimeMillis() - startTime
   // Logger.info(context,"GeneratorUtils.addCreateContenido took ${timeDiff} ms",levelOfLog)

}



private static ArrayList<HashMap> getParametrosConfigMethodsFromFiles(levelOfLog,  technology, pathConfi, pathMethods, methodsOk = false, configOk = false){

    //Logger.info(context,"Into GeneratorUtils.getParametrosConfigMethodsFromFiles",levelOfLog)


    def paramsParametrosArrayTmp = new ArrayList<HashMap>()

    String fileConfig = pathConfi+"config2.json"
    String fileMethod = pathMethods+"methods.json"

    //Logger.info(context,"pathConfi is: ${pathConfi} and pathMethods is ${pathMethods}",levelOfLog)

    //Logger.info(context,"fileConfig is: ${fileConfig} and fileMethod is ${fileMethod}",levelOfLog)


    //Split techonology for AND or IOS
    String[] partsTechnology = technology.split("\\.")

    //For config.json

        def paramsParametrosTemp= [:]
        //Add fichero entry
        paramsParametrosTemp.put("fichero", "config_"+partsTechnology[0])
        //paramsParametrosTemp.put("contenido", this.parseFileToParametrosMap(levelOfLog, new File("/var/lib/jenkins/workspace/test-pipeline-sum-node/jenkins-tests/config.json")))
        paramsParametrosTemp.put("contenido", this.parseFileToParametrosMap(levelOfLog, new JsonSlurperClassic().parse("http://www.alejoestevez.com/config.json".toURL())))


        paramsParametrosArrayTmp.add( paramsParametrosTemp )

    //For method.json

        def paramsParametrosTempMethods= [:]

    paramsParametrosTempMethods.put("fichero", "methods_"+partsTechnology[0])
    //paramsParametrosTempMethods.put("contenido", this.parseFileToParametrosMap(levelOfLog, new File("/var/lib/jenkins/workspace/test-pipeline-sum-node/jenkins-tests/methods.json") ))
    paramsParametrosTemp.put("contenido", this.parseFileToParametrosMap(levelOfLog, new JsonSlurperClassic().parse("http://www.alejoestevez.com/config.json".toURL())))
    paramsParametrosArrayTmp.add( paramsParametrosTempMethods )

    return paramsParametrosArrayTmp

}
