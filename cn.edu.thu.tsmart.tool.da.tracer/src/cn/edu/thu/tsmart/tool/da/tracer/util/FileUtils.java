package cn.edu.thu.tsmart.tool.da.tracer.util;

import java.io.File;
import java.io.IOException;

public class FileUtils {
	
	public static File ensureFile(String fileDir) throws IOException{
		File file = new File(fileDir);
		if(!file.exists()){
			File parentdir = file.getParentFile();
			parentdir.mkdirs();
			file.createNewFile();
		}
		return file;
	}

}
