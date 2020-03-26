package com.nusinfineon.core;

import static com.nusinfineon.util.FlexScriptDefaultCodes.GET_PROCESS_TIME_CODE;
import static com.nusinfineon.util.FlexScriptDefaultCodes.MAIN_15_CODE;
import static com.nusinfineon.util.FlexScriptDefaultCodes.ON_RUN_STOP_CODE;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.getFullPath;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Class to generate a server to connect with FlexSim for running the simulation runs
 */
public class RunCore {

    private static final Logger LOGGER = Logger.getLogger(RunCore.class.getName());

    private String flexsimLocation;
    private String modelLocation;
    private String inputLocation;
    private String inputFile;
    private String outputLocation;
    private String outputFile;
    private boolean isModelShown;
    private String scriptFilepath = "./script.txt";
    private File scriptFile;
    private int currentRunNum;
    private String excelOutputFileName;
    private ArrayList<File> excelInputFiles;
    private ArrayList<File> excelOutputFiles;
    private ArrayList<Integer> listOfMinBatchSizes;
    private ScriptGenerator scriptGenerator;
    private Server server;


    public RunCore(String flexsimLocation, String modelLocation, String outputLocation,
                   String runSpeed, String stopTime, boolean isModelShown) {

        this.flexsimLocation = flexsimLocation;
        this.modelLocation = modelLocation;
        outputFile = getBaseName(outputLocation) + "." + getExtension(outputLocation);
        this.outputLocation = getFullPath(outputLocation).replace("\\", "\\\\\\\\\\");
        this.isModelShown = isModelShown;
        scriptGenerator = new ScriptGenerator(runSpeed, stopTime);
        server = new Server(1880);
        currentRunNum = 0;
    }

    /**
     * Main execute function to start runs
     */
    public ArrayList<File> executeRuns(ArrayList<File> excelInputFiles) {
        excelOutputFiles = new ArrayList<File>();
        this.excelInputFiles = excelInputFiles;

        // Iterate through list of runs and run the model with server to establish connection with FlexSim
        while (currentRunNum <= excelInputFiles.size()-1) {
            runModel();
            server.checkForConnection();
            excelOutputFiles.add(new File(getFullPath(outputLocation) + excelOutputFileName + ".xlsx"));
            currentRunNum++;
        }
        int i = 1;
        for (File iter : excelOutputFiles) {
            LOGGER.info("output file "+  i + ": " + iter.toString());
            i++;
        }
        return excelOutputFiles;
    }

    /**
     * Main code the runs the program
     */
    public void runModel() {
        LOGGER.info("Input file path: " + excelInputFiles.get(currentRunNum).toString());
        String tempInputFile = excelInputFiles.get(currentRunNum).toString();
        inputFile =  getBaseName(tempInputFile) + "." + getExtension(tempInputFile);
        inputLocation = getFullPath(tempInputFile).replace("\\", "\\\\");
        excelOutputFileName = getBaseName(inputFile).substring(0,getBaseName(inputFile).lastIndexOf("_")) + "_output";
        deleteExistingFile(getFullPath(outputLocation) + excelOutputFileName + ".xlsx");

        try {
           scriptFile = scriptGenerator.generateScript(inputLocation, inputFile, outputLocation,
                   outputFile, excelOutputFileName);
            Runtime.getRuntime().exec(commandLineGenerator(isModelShown));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Creates the commandline to execute model
     * @throws IOException
     * @return command
     */
    public String commandLineGenerator(boolean isModelShown) {
        String command = '"' + flexsimLocation + '"' +
                '"' + modelLocation + '"' + " /maintenance " + (isModelShown ? "" : "nogui_") + "runscript " +
                "/scriptpath "  + '"' + scriptFile.getAbsolutePath() + '"' ;
        return command;
    }

    /**
     * Deletes existing output files to prevent excel overwrite popup
     * @param pathname
     */
    public void deleteExistingFile(String pathname) {
        try {
            File f = new File(pathname);                         //file to be delete
            if (f.delete()) {                                    //returns Boolean value
                LOGGER.info( f.getName() + " was deleted");   //getting and printing the file name
            } else {
                LOGGER.info(pathname.replace("\\\\\\\\\\", "\\") + " already doesn't exist");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}