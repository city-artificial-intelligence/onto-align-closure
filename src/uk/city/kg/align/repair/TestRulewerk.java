package uk.city.kg.align.repair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.semanticweb.rulewerk.core.model.api.Constant;
import org.semanticweb.rulewerk.core.model.api.Fact;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.model.api.Predicate;
import org.semanticweb.rulewerk.core.model.api.QueryResult;
import org.semanticweb.rulewerk.core.model.api.Variable;
import org.semanticweb.rulewerk.core.model.implementation.Expressions;
import org.semanticweb.rulewerk.core.reasoner.KnowledgeBase;
import org.semanticweb.rulewerk.core.reasoner.QueryResultIterator;
import org.semanticweb.rulewerk.examples.ExamplesUtils;
import org.semanticweb.rulewerk.parser.ParsingException;
import org.semanticweb.rulewerk.parser.RuleParser;
import org.semanticweb.rulewerk.rdf.RdfModelConverter;
import org.semanticweb.rulewerk.reasoner.vlog.VLogReasoner;

public class TestRulewerk {
	
	public static void example1()
		throws IOException, RDFParseException, RDFHandlerException, URISyntaxException, ParsingException {
		
		RdfModelConverter rdfModelConverter = new RdfModelConverter();
		KnowledgeBase kb = new KnowledgeBase();

        // Predicates
        Predicate person = Expressions.makePredicate("person", 1);
        Predicate parent = Expressions.makePredicate("parent", 2);
        Predicate ancestor = Expressions.makePredicate("ancestor", 2);

        // Constants
        Constant mario = Expressions.makeAbstractConstant("mario");
        Constant alice = Expressions.makeAbstractConstant("alice");
        Constant bob = Expressions.makeAbstractConstant("bob");
        
        
        kb.addStatement(Expressions.makeFact(person, alice));
        kb.addStatement(Expressions.makeFact(person, bob));
        kb.addStatement(Expressions.makeFact(parent, alice, bob));
        kb.addStatement(Expressions.makeFact(parent, bob, mario));
        
        //RuleParser.parseDataSourceDeclaration(rules)        
        
        final PositiveLiteral ancestorXY = RuleParser.parsePositiveLiteral("ancestor(?X, ?Y)");
        final PositiveLiteral ancestorXZ = RuleParser.parsePositiveLiteral("ancestor(?X, ?Z)");
        final PositiveLiteral ancestorYZ = RuleParser.parsePositiveLiteral("ancestor(?Y, ?Z)");
        final PositiveLiteral parentXY = RuleParser.parsePositiveLiteral("parent(?X, ?Y)");
        
               
        
        // Variables
        //Variable x = Expressions.new Variable("x");
        //Variable y = new Variable("y");
        //Variable z = new Variable("z");

        // Rule:
        // Ancestor(?x, ?y) :- Parent(?x, ?y).
        kb.addStatement(Expressions.makeRule(ancestorXY, parentXY));

        // Rule:
        // Ancestor(?x, ?z) :- Ancestor(?x, ?y), Ancestor(?y, ?z).
        kb.addStatement(Expressions.makeRule(ancestorXZ, ancestorXY, ancestorYZ));

        // Reason
        try (VLogReasoner reasoner = new VLogReasoner(kb)) {
			reasoner.reason();
                
	             // Execute query
			QueryResultIterator resultIter = reasoner.answerQuery(ancestorXY, true);
	
	        // Iterate over the answers
	        while (resultIter.hasNext()) {
	            QueryResult result = resultIter.next();
	
	            System.out.println(
	                "?x = " + result.getTerms().get(0) +
	                ", ?y = " +  result.getTerms().get(1)
	            );
	        }
        }
		
	}
	
	
	
	public static void example2()	
			throws IOException, RDFParseException, RDFHandlerException, URISyntaxException, ParsingException {
		
		RdfModelConverter rdfModelConverter = new RdfModelConverter();
		KnowledgeBase kb = new KnowledgeBase();
		 
        final File rdfXMLResourceFile = new File(ExamplesUtils.INPUT_FOLDER + "test/subclassof-statements.ttl");
		final FileInputStream inputStream = new FileInputStream(rdfXMLResourceFile);
        final Model rdfModelLoaded = parseRdfResource(inputStream, rdfXMLResourceFile.toURI(), RDFFormat.TURTLE); //RDFFormat.RDFXML

        
        
		/*
		 * Using rulewerk-rdf library, we convert RDF Model triples to facts, each
		 * having the ternary predicate "TRIPLE".
		 */
		final Set<Fact> tripleFacts = rdfModelConverter.rdfModelToFacts(rdfModelLoaded);
		
		
		System.out.print(tripleFacts.size());
		kb.addStatements(tripleFacts);
		
		
		final String rules = "%%%% We specify the rules syntactically for convenience %%%\n"
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ."
				+ "@prefix ex: <http://example.org/o1#> ."
				+ "some(ex:writes, ex:Monograph) ."
				+ "min(ex:writes, ex:Monograph, 1) ."
				//+ "TRIPLE(?C,  rdfs:subClassOf ,?E) :- "
				//+ "  TRIPLE(?C,  rdfs:subClassOf ,?D), TRIPLE(?D,  rdfs:subClassOf ,?E) ."  //Required? Or is it already inferred? Perhaps for the Second iteration not this one
				+ "some(?R, ?D) :- "
				+ "  some(?R, ?C), TRIPLE(?C,  rdfs:subClassOf ,?D) ."
				+ "min(?R, ?D, ?card) :- "
				+ "  min(?R, ?C, ?card), TRIPLE(?C,  rdfs:subClassOf ,?D) .";
		
				
		kb.addStatements(RuleParser.parse(rules).getStatements());  //all
		
		
		
		//rdfs:subClassOf [ a owl:Restriction ; owl:onProperty :writes ; owl:someValuesFrom :Monograph ] .
		//RuleParser.parseFact("some(ex:writes, ex:Monograph)");
		
        //Expressions.makeRule(null, null)
		
		
		final PositiveLiteral someXY = RuleParser.parsePositiveLiteral("some(?X, ?Y)");
		final PositiveLiteral minXYZ = RuleParser.parsePositiveLiteral("min(?X, ?Y, ?Z)");
		
		// Reason
        try (VLogReasoner reasoner = new VLogReasoner(kb)) {
			reasoner.reason();
                
	             // Execute query
			QueryResultIterator resultIter = reasoner.answerQuery(someXY, true);
	
	        // Iterate over the answers
	        while (resultIter.hasNext()) {
	            QueryResult result = resultIter.next();
	
	            System.out.println(
	                "?x = " + result.getTerms().get(0) +
	                ", ?y = " +  result.getTerms().get(1)
	            );
	        }
	        
	        // Execute query
	  			 resultIter = reasoner.answerQuery(minXYZ, true);
	  	
	  	        // Iterate over the answers
	  	        while (resultIter.hasNext()) {
	  	            QueryResult result = resultIter.next();
	  	
	  	            System.out.println(
	  	                "?x = " + result.getTerms().get(0) +
	  	                ", ?y = " +  result.getTerms().get(1) +
	  	                ", ?z = " +  result.getTerms().get(2)
	  	            );
	  	        }
	        
        }

		
	}
	
	
	public static void main(final String[] args)
			throws IOException, RDFParseException, RDFHandlerException, URISyntaxException, ParsingException {
		
		
		
		
		/*KnowledgeBase kb;
		try {
			kb = RuleParser.parse(rules);
		} catch (final ParsingException e) {
			System.out.println("Failed to parse rules: " + e.getMessage());
			return;
		}
		kb.addStatements(tripleFactsISWC2016);
		kb.addStatements(tripleFactsISWC2017);

	
		try (VLogReasoner reasoner = new VLogReasoner(kb)) {
			reasoner.reason();

			/* We query for persons whose organization name is "TU Dresden" . 
			final Constant constantTuDresden = Expressions.makeDatatypeConstant("TU Dresden",
					"http://www.w3.org/2001/XMLSchema#string");
			/* hasOrganizationName(?person, "TU Dresden") 
			final PositiveLiteral queryTUDresdenParticipantsAtISWC = Expressions
					.makePositiveLiteral(predicateHasOrganizationName, varPerson, constantTuDresden);

			System.out.println("\nParticipants at ISWC'16 and '17 from Organization 'TU Dresden':");
			System.out.println("(Answers to query " + queryTUDresdenParticipantsAtISWC + ")\n");
			try (QueryResultIterator queryResultIterator = reasoner.answerQuery(queryTUDresdenParticipantsAtISWC,
					false)) {
				queryResultIterator.forEachRemaining(answer -> System.out
						.println(" - " + answer.getTerms().get(0) + ", organization " + answer.getTerms().get(1)));
			}

		}
		*/
		
		
		//example1();
		example2();
		
		

		
		
	}
	
	
	/**
	 * Parses the data from the supplied InputStream, using the supplied baseURI to
	 * resolve any relative URI references.
	 *
	 * @param inputStream The content to be parsed, expected to be in the given
	 *                    {@code rdfFormat}.
	 * @param baseURI     The URI associated with the data in the InputStream.
	 * @param rdfFormat   The expected RDFformat of the inputStream resource that is
	 *                    to be parsed.
	 * @return A Model containing the RDF triples. Blanks have unique ids across
	 *         different models.
	 * @throws IOException         If an I/O error occurred while data was read from
	 *                             the InputStream.
	 * @throws RDFParseException   If the parser has found an unrecoverable parse
	 *                             error.
	 * @throws RDFHandlerException If the configured statement handler has
	 *                             encountered an unrecoverable error.
	 */
	private static Model parseRdfResource(final InputStream inputStream, final URI baseURI, final RDFFormat rdfFormat)
			throws IOException, RDFParseException, RDFHandlerException {
		final Model model = new LinkedHashModel();
		final RDFParser rdfParser = Rio.createParser(rdfFormat);
		rdfParser.setRDFHandler(new StatementCollector(model));
		rdfParser.parse(inputStream, baseURI.toString());

		return model;
	}

	

}
