package br.cin.ufpe.nlp.ssense.tagger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import br.cin.ufpe.nlp.util.AnnotatedDocument;
import br.cin.ufpe.nlp.util.AnnotatedSentence;
import br.cin.ufpe.nlp.util.AnnotatedToken;
import br.cin.ufpe.nlp.util.AnnotationException;
import br.cin.ufpe.nlp.util.TokenAnnotation;
import edu.cmu.ark.DiscriminativeTagger;
import edu.cmu.ark.LabeledSentence;
import edu.cmu.ark.SuperSenseFeatureExtractor;

@RestController
public class SuperSenseTaggerController {

	@Value("${properties ?:tagger.properties}")
	private String properties;
	
	@Value("${model}")
	private String model;
	
	@Value("${debug ?: false}")
	private boolean debug;
	
	private DiscriminativeTagger tagger;
	
	@PostConstruct
	public void initTagger() {
		if(model == null){
			System.err.println("need to specify --model");
			System.exit(0);
		}
		Resource resource = new PathResource(new File(properties).toURI());
		InputStream is;
		try {
			is = resource.getInputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IllegalStateException("Could not load properties", e);
		}
		DiscriminativeTagger.loadProperties(is);
		this.tagger = DiscriminativeTagger.loadModel(model);
		
	}
	
	@RequestMapping(value="/supersenses/", method=RequestMethod.POST)
	public ResponseEntity<AnnotatedDocument> addSuperSenses(@RequestBody AnnotatedDocument document) throws AnnotationException {
		for (AnnotatedSentence annotatedSentence : document.getSentences()) {
			LabeledSentence inputSentence = new LabeledSentence();
			for (AnnotatedToken	annotatedToken : annotatedSentence.getTokens()) {
				final String tokenText = annotatedToken.getTokenText();
				final String pos = annotatedToken.getAnnotations().get(TokenAnnotation.POSTAG);
				if (pos == null) {
					throw new AnnotationException("Token '" + tokenText + "' did not contain POS annotation");
				}
				inputSentence.addToken(tokenText, SuperSenseFeatureExtractor.getInstance().getStem(tokenText, pos), pos, "0");
			}
			if (inputSentence.length() > 0) {
				tagger.findBestLabelSequenceViterbi(inputSentence, tagger.getWeights());
				final Iterator<String> tokenIterator = inputSentence.getTokens().iterator();
				final Iterator<String> labelIterator = inputSentence.getPredictions().iterator();
				assert (inputSentence.getTokens().size() == annotatedSentence.getTokens().size());
				for (AnnotatedToken annotatedToken : annotatedSentence.getTokens()) {
					String tokenString = annotatedToken.getTokenText();
					assert(tokenString.equals(tokenIterator.next()));
					annotatedToken.addAnnotation(TokenAnnotation.SUPERSENSE, labelIterator.next());
				}
			}
		}
		return new ResponseEntity<AnnotatedDocument>(document, HttpStatus.OK);
	}
}
