package cn.edu.thu.tsmart.tool.da.core.search.sc;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class JdtAstUtil {
    /**
    * get compilation unit of source code
    * @param javaFilePath 
    * @return CompilationUnit
    */
    public static CompilationUnit getCompilationUnit(String javaFilePath){
        byte[] input = null;
		try {
		    BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(javaFilePath));
		    input = new byte[bufferedInputStream.available()];
	            bufferedInputStream.read(input);
	            bufferedInputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

        /** @copy 这里从 JLS3 改成 JLS8*/
	ASTParser astParser = ASTParser.newParser(AST.JLS8);
        astParser.setSource(new String(input).toCharArray());
//        郭 & http://www.programcreek.com/2014/01/how-to-resolve-bindings-when-using-eclipse-jdt-astparser/
        astParser.setResolveBindings(true);
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        //
        astParser.setBindingsRecovery(true);

        CompilationUnit result = (CompilationUnit) (astParser.createAST(null));
        
        return result;
    }
}