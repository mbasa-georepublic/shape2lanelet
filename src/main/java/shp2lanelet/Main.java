/**
 * パッケージ名：shp2lanelet
 * ファイル名  ：Main.java
 * 
 * @author mbasa
 * @since Sep 17, 2024
 */
package shp2lanelet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import shp2lanelet.converter.Shape2Laneltet;
/**
 * 説明：
 *
 */
public class Main {

    /**
     * コンストラクタ
     *
     */
    public Main() {
    }

    /**
     * 
     * 
     * @param args
     */
    public static void main(String[] args) {

        Options options = new Options();
        options.addOption("i", true, "Input ShapeFile to be converted");
        options.addOption("un", true, "User Name of the Editor");
        options.addOption("ui", true, "User ID number of the Editor");
        options.addOption("v", true, "Version ID number");
        options.addOption("l", false, "ShapeFile processed as Lanelet");
        options.addOption("h", false, "Help Message");

        HelpFormatter hf = new HelpFormatter();

        CommandLine cli;
        CommandLineParser parser = new DefaultParser();

        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            hf.printHelp("shp2lanelet [opts]", options);
            return;
        }

        if (cli.hasOption("h") || !cli.hasOption("i") || cli.hasOption("l")) {
            hf.printHelp("shp2lanelet [opts]", options);
            return;
        }

        String shapeFile = cli.getOptionValue("i");
        String userName = cli.getOptionValue("un");
        String userId = cli.getOptionValue("ui");
        String versionId = cli.getOptionValue("v");
        boolean proecssAsLanelet = cli.hasOption("l");

        Shape2Laneltet s2l = new Shape2Laneltet(shapeFile,
                proecssAsLanelet, userName, userId, versionId);
        s2l.process();
    }

}
