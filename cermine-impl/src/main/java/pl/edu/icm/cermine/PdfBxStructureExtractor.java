package pl.edu.icm.cermine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import pl.edu.icm.cermine.exception.AnalysisException;
import pl.edu.icm.cermine.structure.*;
import pl.edu.icm.cermine.structure.model.BxDocument;
import pl.edu.icm.cermine.structure.tools.BxModelUtils;


/**
 * Document geometric structure extractor. Extracts the geometric hierarchical structure
 * (pages, zones, lines, words and characters) from a PDF file and stores it as a BxDocument object.
 *
 * @author Dominika Tkaczyk
 */
public class PdfBxStructureExtractor implements DocumentStructureExtractor {

    /** individual character extractor */
    private CharacterExtractor characterExtractor;
    
    /** document object segmenter */
    private DocumentSegmenter documentSegmenter;
    
    /** reading order resolver */
    private ReadingOrderResolver roResolver;
    
    /** iniial zone classifier */
    private ZoneClassifier initialClassifier;


    public PdfBxStructureExtractor() throws AnalysisException {
        characterExtractor = new ITextCharacterExtractor();
        documentSegmenter = new DocstrumSegmenter();
        roResolver = new HierarchicalReadingOrderResolver();
        
        InputStreamReader modelISRI = new InputStreamReader(PdfBxStructureExtractor.class
				.getResourceAsStream("/pl/edu/icm/cermine/structure/svm_initial_classifier"));
        BufferedReader modelFileI = new BufferedReader(modelISRI);
        InputStreamReader rangeISRI = new InputStreamReader(PdfBxStructureExtractor.class
		        .getResourceAsStream("/pl/edu/icm/cermine/structure/svm_initial_classifier.range"));
        BufferedReader rangeFileI = new BufferedReader(rangeISRI);
        initialClassifier = new SVMInitialZoneClassifier(modelFileI, rangeFileI);
    }
    
    public PdfBxStructureExtractor(InputStream model, InputStream range) throws AnalysisException {
        characterExtractor = new ITextCharacterExtractor();
        documentSegmenter = new DocstrumSegmenter();
        roResolver = new HierarchicalReadingOrderResolver();
        
        InputStreamReader modelISRI = new InputStreamReader(model);
        BufferedReader modelFileI = new BufferedReader(modelISRI);
        InputStreamReader rangeISRI = new InputStreamReader(range);
        BufferedReader rangeFileI = new BufferedReader(rangeISRI);
        initialClassifier = new SVMInitialZoneClassifier(modelFileI, rangeFileI);
    }

    public PdfBxStructureExtractor(CharacterExtractor glyphExtractor, DocumentSegmenter pageSegmenter, 
            ReadingOrderResolver roResolver, ZoneClassifier initialClassifier) {
        this.characterExtractor = glyphExtractor;
        this.documentSegmenter = pageSegmenter;
        this.roResolver = roResolver;
        this.initialClassifier = initialClassifier;
    }
    
    /**
     * Extracts the geometric structure from a PDF file and stores it as BxDocument.
     * 
     * @param stream
     * @return BxDocument object storing the geometric structure
     * @throws AnalysisException 
     */
    @Override
    public BxDocument extractStructure(InputStream stream) throws AnalysisException {
        BxDocument doc = characterExtractor.extractCharacters(stream);
        doc = documentSegmenter.segmentDocument(doc);
        BxModelUtils.setParents(doc);
        doc = roResolver.resolve(doc);
        return initialClassifier.classifyZones(doc);
    }

    public void setGlyphExtractor(CharacterExtractor glyphExtractor) {
        this.characterExtractor = glyphExtractor;
    }

    public void setInitialClassifier(ZoneClassifier initialClassifier) {
        this.initialClassifier = initialClassifier;
    }

    public void setPageSegmenter(DocumentSegmenter pageSegmenter) {
        this.documentSegmenter = pageSegmenter;
    }

    public void setRoResolver(ReadingOrderResolver roResolver) {
        this.roResolver = roResolver;
    }
   
}