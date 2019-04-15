package br.com.jkstatus.prometheus

class Gauge {
    String help
    String name

    Gauge(String help, String name) {
        this.help = help
        this.name = name
    }

    void parse(PrintWriter out, Map values, Float value = 1.0) {
        def parseMap = values?.collect { k, v -> "$k=\"$v\"" }.join(',')
        parseMap = parseMap ? "{$parseMap}" : ""
        out.println """# HELP ${name} ${help}
# TYPE ${name} gauge
${name}${parseMap} ${value}"""
    }

    void parse(PrintWriter out, Map nameValues, List<Value> values) {
        out.println """# HELP ${name} ${help}
# TYPE ${name} gauge"""
        values*.parse(this, out, nameValues)
    }

    static class Value {
        String type
        Float value

        Value(String type, Float value){
            this.type = type
            this.value = value
        }

        void parse(Gauge gauge, PrintWriter out, Map nameValues) {
            nameValues.type = type
            def parseMap = nameValues?.sort{ ["type","name"].contains(it.key)?1:0 }.collect { k, v -> "$k=\"$v\"" }.join(',')
            out.println("${gauge.name}{${parseMap}} ${value}")
        }
    }
}


