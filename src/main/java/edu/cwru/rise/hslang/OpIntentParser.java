package edu.cwru.rise.hslang;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.PredictionMode;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import edu.cwru.rise.hslang.parser.HSlangLexer;
import edu.cwru.rise.hslang.parser.HSlangParser;

public class OpIntentParser {
    public static void main(String[] args) {
        try {
            // Create a scanner that reads from the input stream passed to us
            String file = "contracts/DelegateAdmin/test.hsl";
            //String file = "contracts/Finance/option.hsl";
            //String file = "contracts/CrypotAsset/asset_simplified.hsl";
            CharStream charStream = new ANTLRInputStream(new String(Files.readAllBytes(Paths.get(file))));
            Lexer lexer = new HSlangLexer(charStream);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
			//long start = System.currentTimeMillis();
			tokens.fill(); // load all and check time


            // Create a parser that reads from the scanner
            HSlangParser parser = new HSlangParser(tokens);
            parser.addErrorListener(new DiagnosticErrorListener());
            parser.setErrorHandler(new BailErrorStrategy());
            parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

            // start parsing at the compilationUnit rule
            ParserRuleContext t = parser.sourceFile();
            parser.setBuildParseTree(false);
            //System.out.println("tree:" + t.toStringTree(parser));

            OpIntentVisitor visitor = new OpIntentVisitor();

            visitor.visit(t);

            // Print dependencies
            String dep = visitor.res.toString();
            if(dep.equals("\"dependencies\":[")){
                throw new HSLParsingException("Wrong dependencies");
            }

            for(Map.Entry<String, int[]> entry : visitor.accMax.entrySet()){
                if(entry.getValue()[1] < 0){
                    entry.getValue()[1] = 0;
                }
                System.out.println("Account " + entry.getKey() +": " + entry.getValue()[1]);
            }

            String eol = ",\n";
            int eolLen = eol.length();
            String tmp = visitor.output.toString();
            String out = tmp.substring(0,tmp.length()-eolLen);
            System.out.println(out + "\n]");
            dep = dep.substring(0, dep.length()-eolLen);
            System.out.println(dep + "\n]");

            StringBuilder account = visitor.accountMap;
            StringBuilder contract = visitor.contractMap;
            account.setLength(account.length()-2);
            contract.setLength(contract.length()-2);
            StringBuilder res = new StringBuilder();
            res.append("\"contracts\":[\n");
            res.append(contract);
            res.append("]\n");
            System.out.print(res.toString());
            res.setLength(0);
            res.append("\"accounts\":[\n");
            res.append(account);
            res.append("]\n");
            System.out.println(res.toString());

            /*
            long stop = System.currentTimeMillis();
            long lexerTime = stop-start;
            System.out.println("time: "+ lexerTime);
            */

        } catch (Exception e) {
            System.err.println("parser exception: " + e);
            e.printStackTrace();   // so we can get stack trace
        }
    }
}
