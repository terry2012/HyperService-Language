package edu.cwru.rise.solidity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import edu.cwru.rise.hslang.structure.*;
import edu.cwru.rise.hslang.structure.Modifier;
import edu.cwru.rise.solidity.parser.SolidityBaseVisitor;
import edu.cwru.rise.solidity.parser.SolidityParser;

public class SolOpIntentVisitor extends SolidityBaseVisitor {

    public HashMap<String, Type> types = new HashMap<>(); // store contracts' type
    public HashMap<String, Contract> contracts = new HashMap<>(); // store all imported contracts
    Contract curr;
    int i = 0; // record the position of a field
    HashMap<String, List<Field>> structs = new HashMap<>(); // store structs

    /**
     * find the contract and store contract's name
     * @param ctx contract declaration
     * @return object
     */
    @Override
    public Object visitContractDefinition(SolidityParser.ContractDefinitionContext ctx) {
        Contract contract = new Contract();
        contract.name = ctx.identifier().getText();
        curr = contract;

        contracts.put(contract.name, contract);
        return super.visitContractDefinition(ctx);
    }

    /**
     * find the struct and store it into the map
     * @param ctx struct declaration
     * @return object
     */
    @Override
    public Object visitStructDefinition(SolidityParser.StructDefinitionContext ctx) {
        Struct t = new Struct();
        t.name = ctx.identifier().getText();
        List<Field> tmp = new ArrayList<>();
        for (SolidityParser.VariableDeclarationContext vdecl : ctx.variableDeclaration()) {
            Field f = new Field();
            f.type = findType(vdecl.typeName().getText());
            f.name = vdecl.identifier().getText();
            f.pos = "null";
            tmp.add(f);
        }
        structs.put(t.name,tmp);
        types.put(t.name, t);
        return super.visitStructDefinition(ctx);
    }


    /**
     * find the state variable and store it into the map
     * @param ctx State variable declaration
     * @return object
     */
    @Override
    public Object visitStateVariableDeclaration(SolidityParser.StateVariableDeclarationContext ctx) {
        String typename = ctx.typeName().getText();
        Type t = findType(typename);
        Field f = new Field();
        f.name = ctx.identifier().getText();
        f.type = t;
        f.pos = posLocation(typename);
        curr.fields.add(f);

        return super.visitStateVariableDeclaration(ctx);
    }

    /**
     * find the storage posistion of the variable
     * @param typename variable
     * @return the storage position of the variable
     */
    private String posLocation(String typename){
        String tmp = Integer.toString(i);
        StringBuffer res = new StringBuffer(tmp);
        // array variable
        if(typename.contains("[") && typename.contains("]")){
            String size = typename.substring(typename.indexOf("[") + 1, typename.indexOf("]")).trim();
            if (size.length() != 0) {
                if(structs.containsKey(typename)){
                    int counts = structs.get(typename).size()-1;
                    i += counts * Integer.parseInt(size);
                    res.append("-" + i);
                }
                else {
                    i += Integer.parseInt(size);
                    res.append('-');
                }
                i--;
            }
        }
        else{
            if(structs.containsKey(typename)){
                i += structs.get(typename).size()-1;
                res.append("-" + i);
                i--;
            }
        }
        i++;
        return res.toString();
    }

    @Override
    public Object visitFunctionDefinition(SolidityParser.FunctionDefinitionContext ctx) {
        Function f = new Function();
        if (ctx.identifier() != null) {
            f.name = ctx.identifier().getText();
        }else{
            f.name = "_callback_";
        }
        for (SolidityParser.ParameterContext pc : ctx.parameterList().parameter()) {
            Parameter p = new Parameter();
            p.type = findType(pc.typeName().getText());
            p.name = pc.identifier().getText();
            f.args.add(p);
        }

        if (ctx.returnParameters() != null) {
            for (SolidityParser.ParameterContext pc : ctx.returnParameters().parameterList().parameter()) {
                Parameter p = new Parameter();
                p.type = findType(pc.typeName().getText());
                if (pc.identifier() != null) {
                    p.name = pc.identifier().getText();
                }
                f.returns.add(p);
            }
        }

        if (ctx.modifierList().PublicKeyword().size() > 0) {
            f.modifiers.add(Modifier.PUBLIC);
        }

        if (ctx.modifierList().PrivateKeyword().size() > 0) {
            f.modifiers.add(Modifier.PRIVATE);
        }
        if (ctx.modifierList().ExternalKeyword().size() > 0) {
            f.modifiers.add(Modifier.EXTERNAL);
        }
        if (ctx.modifierList().InternalKeyword().size() > 0) {
            f.modifiers.add(Modifier.INTERNAL);
        }
        for (SolidityParser.StateMutabilityContext sm : ctx.modifierList().stateMutability()) {
            switch (sm.getText()) {
                case "view":
                    f.modifiers.add(Modifier.VIEW);
                    break;
                case "payable":
                    f.modifiers.add(Modifier.PAYABLE);
                    break;
                case "constant":
                    f.modifiers.add(Modifier.CONSTANT);
                    break;
                case "pure":
                    f.modifiers.add(Modifier.PURE);
                    break;
            }
        }

        curr.functions.add(f);
        return super.visitFunctionDefinition(ctx);
    }

    /**
     * mapping the type to our defined type
     * @param typename input type
     * @return self defined type
     */
    private Type findType(String typename) {
        if(typename.equals("int") && !typename.contains("uint")){
            typename = typename.replaceAll("int", "uint");
        }
        typename = typename.replaceAll("addresspayable", "address");

        if (types.containsKey(typename)) {
            return types.get(typename);
        }
        Type t = new Type();
        // map type
        if (typename.contains("mapping")) {
            Mapping mapping = new Mapping();

            String fromtypename = typename.substring(typename.indexOf("(") + 1, typename.indexOf("=>")).trim();
            String totypename = typename.substring(typename.indexOf("=>")+2, typename.lastIndexOf(")")).trim();

            Type keytype = findType(fromtypename);
            Type valuetype = findType(totypename);

            mapping.name = typename;
            mapping.keytype = keytype;
            mapping.valuetype = valuetype;

            t = mapping;

        }
        // array type
        else {
            if (typename.contains("[") && typename.contains("]")) {
                Array a = new Array();
                Type elemtype = findType(typename.substring(0, typename.indexOf("[")));
                a.elemType = elemtype;
                String size = typename.substring(typename.indexOf("[") + 1, typename.indexOf("]")).trim();
                if (size.length() != 0) {
                    a.length = Integer.parseInt(size);
                }
                return elemtype;

            } else {
                t.name = typename;
                types.put(typename,t);
            }
        }
        return t;
    }

    /**
     * Get current contract
     * @return contract
     */
    public Contract getContract() {
        return curr;
    }
}