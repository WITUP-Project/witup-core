package br.unb.cic.witup.analysis.expath.records;

import java.util.List;

public record CallSiteKey(
        String callerSignature,
        String calleeSignature,
        String callNodeId,     // id estável do call site (ver abaixo)
        List<String> args) {}
