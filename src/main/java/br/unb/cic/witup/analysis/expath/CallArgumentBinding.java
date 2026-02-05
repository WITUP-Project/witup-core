package br.unb.cic.witup.analysis.expath;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import sootup.core.jimple.basic.Value;

/**
 * formal parameter name -> actual argument (SootUp Value)
 * Example: n -> $i1, sum -> $d0
 */
public record CallArgumentBinding(Map<String, Value> formalParamToActualArg) {

    public CallArgumentBinding {
        Objects.requireNonNull(formalParamToActualArg, "formalParamToActualArg");
        formalParamToActualArg = Collections.unmodifiableMap(formalParamToActualArg);
    }
}


