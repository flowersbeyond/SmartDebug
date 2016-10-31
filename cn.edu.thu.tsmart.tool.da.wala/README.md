## Configuring wala with tsmart-da-tracer project

DATE: 2016.3.22

CONTENT: Procedures to configure wala with tsmart-da project. Currently for the tracer.



### Prerequisites
WALA relies on Java 7 to run. In order to compile all the test code from source, Java 8 is required.



### Getting the code

Suppose the root dir of tsmart is **$TsmartRoot**.

First create dir: **$TsmartRoot/cn.edu.thu.tsmart.tool.da.wala/**

Enter this dir, open git bash and run:
    `<git clone https://github.com/wala/WALA.git>`

This will create a new folder **WALA** under this directory. 



### Building the code
To get started, we concentrate on a core subset of WALA for standard Java analysis:
   * com.ibm.wala.core (core WALA framework support)
   * com.ibm.wala.shrike (Shrike bytecode manipulation library)
   * com.ibm.wala.util (general utilities (post WALA 1.3.2))

### Configuring WALA properties
1. In the com.ibm.wala.core project, you need to copy the file **dat/wala.properties.sample** to **dat/wala.properties**.
2. You need to then edit **wala.properties** to reflect your environment. See the properties file for the detailed instructions on what properties you must set. 

    Note:For beginners, we recommend you set the **java_runtime_dir** property (which is mandatory) and the **output_dir** property (required for some tests below like PDFTypeHierarchy). Note that **the directory specified for output_dir must exist** on the filesystem; WALA will not create it.
