package br.com.jkstatus.prometheus

class Counter {
    String help
    String name

    Counter(String help, String name){
        this.help = help
        this.name = name
    }
    void parse(PrintWriter out, Map values, float counter){
        def parseMap = values?.collect { k,v -> "$k=\"$v\"" }.join(',')
        parseMap = parseMap?"{$parseMap}":""
        out.println """# HELP ${name} ${help}
# TYPE ${name} counter
${name}${parseMap} ${counter}"""
    }
}
