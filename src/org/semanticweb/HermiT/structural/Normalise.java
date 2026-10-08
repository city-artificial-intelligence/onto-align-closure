package org.semanticweb.HermiT.structural;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * java -jar normaliser.jar IN.owl OUT.clauses
 *
 * Runs HermiT's structural normalisation (through the adapted classes in this package) and
 * writes one clause per line, tab-separated, in the literal grammar:
 *   C  lit  lit ...        a disjunction of literals ('+' atom | '-' atom)
 *   P  sub-iri  super-iri  a simple object property inclusion
 */
public final class Normalise {

    public static void main(String[] args) throws Exception {
        /*if (args.length != 2) {
            System.err.println("usage: java -jar normaliser.jar IN.owl OUT.clauses");
            System.exit(2);
        }*/
    	
    	 String userDirectory = FileSystems.getDefault()
  	            .getPath("")
  	           .toAbsolutePath()
  	            .toString();	        
 		 System.out.println(userDirectory);
 		 
 		String file;  //args[0]
 		file =  userDirectory + "/data/witnesses/s3_definition/o1.ttl";
 		//file = "C:/Users/Ernes/Documents/test_norm.owl";
 		
 		String file_norm;  //args[1]
 		//file_norm =  userDirectory + "/data/witnesses/w1/o1_norm.ttl";
 		file_norm = "C:/Users/Ernes/Documents/test_norm-normalised.dl";
    	
    	
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(file));
        OWLAxiomsAdapted ax = new OWLAxiomsAdapted();
        new OWLNormalizationAdapted(manager.getOWLDataFactory(), ax, 0).processOntology(ontology);

        List<String> lines = new ArrayList<>();
        for (List<OWLClassExpression> clause : ax.getNormalisedConceptInclusions()) {
            StringBuilder line = new StringBuilder("C");
            for (OWLClassExpression e : clause) {
                line.append('\t').append(literal(e));
            }
            lines.add(line.toString());
        }
        int skipped = 0;
        for (List<OWLObjectPropertyExpression> pair : ax.getNormalisedObjectPropertyInclusions()) {
            if (pair.get(0) instanceof OWLObjectInverseOf || pair.get(1) instanceof OWLObjectInverseOf) {
                skipped++;
                continue;
            }
            lines.add("P\t" + iri(pair.get(0)) + "\t" + iri(pair.get(1)));
        }
        Files.write(Paths.get(file_norm), lines, StandardCharsets.UTF_8);
        System.err.println("normaliser: " + lines.size() + " clauses written to " + file_norm
                + "; " + skipped + " property inclusions with an inverse skipped");
    }

    /** '+' atom or '-' atom. After HermiT's NNF only named classes should carry '-'. */
    static String literal(OWLClassExpression e) {
        if (e instanceof OWLObjectComplementOf) {
            OWLClassExpression inner = ((OWLObjectComplementOf) e).getOperand();
            return inner instanceof OWLClass ? "-" + iri((OWLClass) inner) : "-other(" + inner + ")";
        }
        return "+" + atom(e);
    }

    static String atom(OWLClassExpression e) {
        if (e instanceof OWLClass) {
            return iri((OWLClass) e);
        }
        if (e instanceof OWLObjectSomeValuesFrom) {
            OWLObjectSomeValuesFrom r = (OWLObjectSomeValuesFrom) e;
            String c = cls(r.getFiller());
            if (c != null) {
                return "some(" + prop(r.getProperty()) + "," + c + ")";
            }
        } else if (e instanceof OWLObjectAllValuesFrom) {
            OWLObjectAllValuesFrom r = (OWLObjectAllValuesFrom) e;
            String c = cls(r.getFiller());
            if (c != null) {
                return "only(" + prop(r.getProperty()) + "," + c + ")";
            }
        } else if (e instanceof OWLObjectMinCardinality) {
            OWLObjectMinCardinality r = (OWLObjectMinCardinality) e;
            String c = cls(r.getFiller());
            if (c != null) {
                return "min(" + r.getCardinality() + "," + prop(r.getProperty()) + "," + c + ")";
            }
        } else if (e instanceof OWLObjectMaxCardinality) {
            OWLObjectMaxCardinality r = (OWLObjectMaxCardinality) e;
            String c = cls(r.getFiller());
            if (c != null) {
                return "max(" + r.getCardinality() + "," + prop(r.getProperty()) + "," + c + ")";
            }
        }
        return "other(" + e + ")";
    }

    /** iri, or '~' iri for the complement of a named class; null when the filler is not simple. */
    static String cls(OWLClassExpression filler) {
        if (filler instanceof OWLClass) {
            return iri((OWLClass) filler);
        }
        if (filler instanceof OWLObjectComplementOf) {
            OWLClassExpression inner = ((OWLObjectComplementOf) filler).getOperand();
            if (inner instanceof OWLClass) {
                return "~" + iri((OWLClass) inner);
            }
        }
        return null;
    }

    static String prop(OWLObjectPropertyExpression p) {
        if (p instanceof OWLObjectInverseOf) {
            return "inv(" + iri(((OWLObjectInverseOf) p).getInverse()) + ")";
        }
        return iri(p);
    }

    static String iri(OWLClass c) {
        return c.getIRI().toString();
    }

    static String iri(OWLObjectPropertyExpression p) {
        return p.asOWLObjectProperty().getIRI().toString();
    }
}
