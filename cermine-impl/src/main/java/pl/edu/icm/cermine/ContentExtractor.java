package pl.edu.icm.cermine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.google.common.collect.Lists;

import pl.edu.icm.cermine.bibref.model.BibEntry;
import pl.edu.icm.cermine.bibref.sentiment.model.CitationPosition;
import pl.edu.icm.cermine.bibref.sentiment.model.CitationSentiment;
import pl.edu.icm.cermine.exception.AnalysisException;
import pl.edu.icm.cermine.exception.TransformationException;
import pl.edu.icm.cermine.metadata.model.DocumentMetadata;
import pl.edu.icm.cermine.structure.model.BxDocument;
import pl.edu.icm.cermine.structure.transformers.BxDocumentToTrueVizWriter;
import pl.edu.icm.cermine.tools.timeout.Timeout;
import pl.edu.icm.cermine.tools.timeout.TimeoutException;
import pl.edu.icm.cermine.tools.timeout.TimeoutRegister;

/**
 * Extracts content from PDF files.
 * <p/>
 * It stores the results of the extraction in various formats.
 * The extraction process is performed only if the requested result
 * is not available yet.
 * <p/>
 * User can set a timeout to limit the processing time of the <code>get*</code>
 * methods of the class.
 * 
 * @author Dominika Tkaczyk
 * @author Mateusz Kobos
 *
 */
public class ContentExtractor {
    private final long SECONDS_TO_MILLIS = 1000;
    
    private final InternalContentExtractor extractor;
    
    private Timeout mainTimeout = new Timeout();
    
    public ContentExtractor() throws AnalysisException{
        this.extractor = new InternalContentExtractor();
    }
    
    /**
     * Set object-bound timeout.
     * <p/>
     * If the deadline time specified by the timeout passes while one
     * of the <code>get*</code> methods of the class is running, the processing
     * stops and the method throws an exception.
     * <p/>
     * In case the <code>get*</code> method defines a timeout by itself, it is
     * treated as an additional restriction (i.e., the effective timeout 
     * deadline is a minimum value of both timeout deadlines).
     * <p/>
     * The value of the timeout is approximate because in some cases,
     * the program might be allowed to slightly exceeded this time,
     * say by a second or two.
     * 
     * @param timeoutSeconds approximate timeout in seconds
     */
    public void setTimeout(long timeoutSeconds){
        this.mainTimeout = new Timeout(timeoutSeconds * SECONDS_TO_MILLIS);
    }
    
    /**
     * Remove the object-bound timeout.
     */
    public void removeTimeout(){
        this.mainTimeout = new Timeout();
    }
    
    /**
     * Stores the input PDF stream.
     * 
     * @param pdfFile PDF stream
     * @throws IOException 
     */
    public void setPDF(InputStream pdfFile) throws IOException {
        this.extractor.setPDF(pdfFile);
    }

    /**
     * Sets the input bx document.
     * 
     * @param bxDocument 
     */
    public void setBxDocument(BxDocument bxDocument) throws IOException {
        this.extractor.setBxDocument(bxDocument);
    }
    
    /**
     * Stores citation locations.
     * 
     * @param citationPositions citation locations
     */
    public void setCitationPositions(List<List<CitationPosition>> citationPositions) {
        this.extractor.setCitationPositions(citationPositions);
    }

    /**
     * Stores the document's raw full text.
     * 
     * @param rawFullText raw full text
     */
    public void setRawFullText(String rawFullText) {
        this.extractor.setRawFullText(rawFullText);
    }

    /**
     * Stores the document's references.
     * 
     * @param references the document's references
     */
    public void setReferences(List<BibEntry> references) {
        this.extractor.setReferences(references);
    }
    
    /**
     * Resets the extraction results.
     * 
     * @throws IOException 
     */
    public void reset() throws IOException {
        this.extractor.reset();
    }

    public ComponentConfiguration getConf() {
        return this.extractor.getConf();
    }

    public void setConf(ComponentConfiguration conf) {
        this.extractor.setConf(conf);
    }
    
    private BxDocument getBxDocument(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getBxDocument();
        } finally {
            TimeoutRegister.remove();
        }        
    }
    
    /**
     * Extracts geometric structure.
     * 
     * @return geometric structure
     * @throws AnalysisException 
     * @throws TimeoutException thrown when timeout deadline has passed. 
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public BxDocument getBxDocument() 
            throws AnalysisException, TimeoutException {
        return getBxDocument(mainTimeout);
    }
    
    /** The same as {@link #getBxDocument()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public BxDocument getBxDocument(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getBxDocument(combineWithMainTimeout(timeoutSeconds));
    }
    
    private Timeout combineWithMainTimeout(long timeoutSeconds){
        Timeout local = new Timeout(timeoutSeconds * SECONDS_TO_MILLIS);
        Timeout t = Timeout.min(mainTimeout, local);
        return t;
    }
    

    private DocumentMetadata getMetadata(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getMetadata();
        } finally {
            TimeoutRegister.remove();
        }        
    }
    
    /**
     * Extracts the metadata.
     * 
     * @return the metadata
     * @throws AnalysisException 
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public DocumentMetadata getMetadata() 
            throws AnalysisException, TimeoutException {
        return getMetadata(mainTimeout);
    }
    
    /** The same as {@link #getMetadata()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public DocumentMetadata getMetadata(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getMetadata(combineWithMainTimeout(timeoutSeconds));
    }

    
    private Element getNLMMetadata(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getNLMMetadata();
        } finally {
            TimeoutRegister.remove();
        }        
    }
    
    /**
     * Extracts the metadata in NLM format.
     * 
     * @return the metadata in NLM format
     * @throws AnalysisException 
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public Element getNLMMetadata() 
            throws AnalysisException, TimeoutException {
        return getNLMMetadata(mainTimeout);
    }
    
    /** The same as {@link #getNLMMetadata()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public Element getNLMMetadata(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getNLMMetadata(combineWithMainTimeout(timeoutSeconds));
    }

    
    private List<BibEntry> getReferences(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getReferences();
        } finally {
            TimeoutRegister.remove();
        }        
    }
    
    /**
     * Extracts the references.
     * 
     * @return the list of references
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public List<BibEntry> getReferences() 
            throws AnalysisException, TimeoutException {
        return getReferences(mainTimeout);
    }
    
    /** The same as {@link #getReferences()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public List<BibEntry> getReferences(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getReferences(combineWithMainTimeout(timeoutSeconds));
    }

    
    private List<Element> getNLMReferences(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getNLMReferences();
        } finally {
            TimeoutRegister.remove();
        }        
    }

    /**
     * Extracts the references in NLM format.
     * 
     * @return the list of references
     * @throws AnalysisException 
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public List<Element> getNLMReferences() 
            throws AnalysisException, TimeoutException {
        return getNLMReferences(mainTimeout);
    }
    
    /** The same as {@link #getNLMReferences()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public List<Element> getNLMReferences(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getNLMReferences(combineWithMainTimeout(timeoutSeconds));
    }

    
    private List<List<CitationPosition>> getCitationPositions(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getCitationPositions();
        } finally {
            TimeoutRegister.remove();
        }        
    }
    
    /**
     * Extracts the locations of the document's citations.
     * 
     * @return the locations
     * @throws AnalysisException 
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public List<List<CitationPosition>> getCitationPositions() 
            throws AnalysisException, TimeoutException {
        return getCitationPositions(mainTimeout);
    }
    
    /** The same as {@link #getCitationPositions()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public List<List<CitationPosition>> getCitationPositions(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getCitationPositions(combineWithMainTimeout(timeoutSeconds));
    }

    
    private List<CitationSentiment> getCitationSentiments(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getCitationSentiments();
        } finally {
            TimeoutRegister.remove();
        }        
    }
    
    /**
     * Extracts the sentiments of the document's citations.
     * 
     * @return the citation sentiments
     * @throws AnalysisException 
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public List<CitationSentiment> getCitationSentiments() 
            throws AnalysisException, TimeoutException {
        return getCitationSentiments(mainTimeout);
    }
    
    /** The same as {@link #getCitationSentiments()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public List<CitationSentiment> getCitationSentiments(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getCitationSentiments(combineWithMainTimeout(timeoutSeconds));
    }


    private String getRawFullText(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getRawFullText();
        } finally {
            TimeoutRegister.remove();
        }        
    }
    
    /**
     * Extracts raw text.
     * 
     * @return raw text
     * @throws AnalysisException 
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public String getRawFullText() 
            throws AnalysisException, TimeoutException {
        return getRawFullText(mainTimeout);
    }
    
    /** The same as {@link #getRawFullText()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public String getRawFullText(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getRawFullText(combineWithMainTimeout(timeoutSeconds));
    }
    
    private Element getLabelledRawFullText(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getLabelledRawFullText();
        } finally {
            TimeoutRegister.remove();
        }        
    }
    
    /**
     * Extracts labeled raw text.
     * 
     * @return labeled raw text
     * @throws AnalysisException 
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public Element getLabelledRawFullText() 
            throws AnalysisException, TimeoutException {
        return getLabelledRawFullText(mainTimeout);
    }
    
    /** The same as {@link #getLabelledRawFullText()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public Element getLabelledRawFullText(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getLabelledRawFullText(combineWithMainTimeout(timeoutSeconds));
    }
    
    private Element getNLMText(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getNLMText();
        } finally {
            TimeoutRegister.remove();
        }        
    }
    
    /**
     * Extracts structured full text.
     * 
     * @return full text in NLM format
     * @throws AnalysisException 
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public Element getNLMText() 
            throws AnalysisException, TimeoutException {
        return getNLMText(mainTimeout);
    }
    
    /** The same as {@link #getNLMText()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public Element getNLMText(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getNLMText(combineWithMainTimeout(timeoutSeconds));
    }
    
    
    private Element getNLMContent(Timeout timeout) 
            throws AnalysisException, TimeoutException {
        try {
            TimeoutRegister.set(timeout);
            TimeoutRegister.get().check();
            return extractor.getNLMContent();
        } finally {
            TimeoutRegister.remove();
        }        
    }
    
    /**
     * Extracts full content in NLM format.
     * 
     * @return full content in NLM format
     * @throws AnalysisException 
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public Element getNLMContent() 
            throws AnalysisException, TimeoutException {
        return getNLMContent(mainTimeout);
    }
    
    /** The same as {@link #getNLMContent()} but with a timeout.
     *  
     * @param timeoutSeconds approximate timeout in seconds
     * @throws AnalysisException
     * @throws TimeoutException thrown when timeout deadline has passed.
     * See {@link #setTimeout(long)} for additional information about the 
     * timeout.
     */
    public Element getNLMContent(long timeoutSeconds) 
            throws AnalysisException, TimeoutException {
        return getNLMContent(combineWithMainTimeout(timeoutSeconds));
    }
    

    public static void main(String[] args) throws ParseException, AnalysisException, IOException, TransformationException {
        CommandLineOptionsParser parser = new CommandLineOptionsParser();
        if (!parser.parse(args)) {
            System.err.println(
                    "Usage: ContentExtractor -path <path> [optional parameters]\n\n"
                  + "Tool for extracting metadata and content from PDF files.\n\n"
                  + "Arguments:\n"
                  + "  -path <path>              path to a PDF file or directory containing PDF files\n"
                  + "  -ext <extension>          (optional) the extension of the resulting metadata file;\n"
                  + "                            default: \"cermxml\"; used only if passed path is a directory\n"
                  + "  -modelmeta <path>         (optional) the path to the metadata classifier model file\n"
                  + "  -modelinit <path>         (optional) the path to the initial classifier model file\n"
                  + "  -str                      whether to store structure (TrueViz) files as well;\n"
                  + "                            used only if passed path is a directory\n"
                  + "  -strext <extension>       (optional) the extension of the structure (TrueViz) file;\n"
                  + "                            default: \"cxml\"; used only if passed path is a directory\n"
                  + "  -threads <num>            number of threads for parallel processing\n"
                  + "  -timeout <seconds>        approximate maximum allowed processing time for a PDF file\n"
                  + "                            in seconds; by default, no timeout is used;\n"
                  + "                            the value is approximate because in some cases,\n"
                  + "                            the program might be allowed to slightly exceeded this time,\n"
                  + "                            say by a second or two;\n"
                  + "                            \n");
            System.exit(1);
        }
        
        String path = parser.getPath();
        String extension = parser.getNLMExtension();
        boolean extractStr = parser.extractStructure();
        String strExtension = parser.getBxExtension();
        InternalContentExtractor.THREADS_NUMBER = parser.getThreadsNumber();
        Long timeoutSeconds = parser.getTimeout();
 
        File file = new File(path);
        if (file.isFile()) {
            try {
                ContentExtractor extractor = new ContentExtractor();
                if (timeoutSeconds != null) {
                    extractor.setTimeout(timeoutSeconds);
                }
                TimeoutRegister.get().check();
                parser.updateMetadataModel(extractor.getConf());
                parser.updateInitialModel(extractor.getConf());
                InputStream in = new FileInputStream(file);
                extractor.setPDF(in);
                TimeoutRegister.get().check();

                Element result = extractor.getNLMContent();
                
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                System.out.println(outputter.outputString(result));
            } catch (Exception ex) {
                printException(ex);
            }
        } else {
        
            Collection<File> files = FileUtils.listFiles(file, new String[]{"pdf"}, true);
    
            int i = 0;
            for (File pdf : files) {
                File xmlF = new File(pdf.getPath().replaceAll("pdf$", extension));
                if (xmlF.exists()) {
                    i++;
                    continue;
                }
 
                long start = System.currentTimeMillis();
                float elapsed = 0;
                
                System.out.println("File processed: "+pdf.getPath());
 
                ContentExtractor extractor = null;
                try {
                    extractor = new ContentExtractor();
                    if (timeoutSeconds != null){
                        extractor.setTimeout(timeoutSeconds);
                    }
                    parser.updateMetadataModel(extractor.getConf());
                    parser.updateInitialModel(extractor.getConf());
                    InputStream in = new FileInputStream(pdf);
                    extractor.setPDF(in);

                    BxDocument doc = extractor.getBxDocument();
                    Element result = extractor.getNLMContent();

                    XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                    if (!xmlF.createNewFile()) {
                        System.out.println("Cannot create new file!");
                    }
                    FileUtils.writeStringToFile(xmlF, outputter.outputString(result));            
                
                    if (extractStr) {
                        BxDocumentToTrueVizWriter writer = new BxDocumentToTrueVizWriter();
                        File strF = new File(pdf.getPath().replaceAll("pdf$", strExtension));
                        writer.write(new FileWriter(strF), Lists.newArrayList(doc));
                    }
                } catch (AnalysisException ex) {
                    printException(ex);
                } catch (TransformationException ex) {
                    printException(ex);
                } catch (TimeoutException ex) {
                    printException(ex);
                } finally {
                    if (extractor != null){
                        extractor.removeTimeout();
                    }
                    long end = System.currentTimeMillis();
                    elapsed = (end - start) / 1000F;
                }
                
                i++;
                int percentage = i*100/files.size();
                System.out.println("Extraction time: " + Math.round(elapsed) + "s");
                System.out.println("Progress: "+percentage + "% done (" + i +" out of " + files.size() + ")");
                System.out.println("");
            }
        }
    }
        
    private static void printException(Exception ex) {
        System.out.print("Exception occured: " + ExceptionUtils.getStackTrace(ex));
    }
}
