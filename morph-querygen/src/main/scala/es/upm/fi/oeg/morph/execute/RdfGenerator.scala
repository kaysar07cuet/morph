package es.upm.fi.oeg.morph.execute
import collection.JavaConversions._
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.query.DatasetFactory
import es.upm.fi.oeg.morph.voc.RDF
import es.upm.fi.oeg.morph.r2rml.R2rmlReader
import es.upm.fi.oeg.morph.querygen.RdbQueryGenerator
import es.upm.fi.oeg.morph.relational.RelationalModel
import java.sql.Types
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype
import java.sql.SQLSyntaxErrorException
import com.hp.hpl.jena.rdf.model.Resource
import com.hp.hpl.jena.sparql.core.DatasetGraph
import es.upm.fi.oeg.morph.r2rml.GraphMap
import com.hp.hpl.jena.query.DataSource
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.Property
import com.hp.hpl.jena.datatypes.RDFDatatype
import es.upm.fi.oeg.morph.r2rml.PredicateObjectMap
import es.upm.fi.oeg.morph.r2rml.LiteralType
import es.upm.fi.oeg.morph.r2rml.IRIType

class RelationalQueryException(msg:String,e:Throwable) extends Exception(msg,e)

class RdfGenerator(model:R2rmlReader,relational:RelationalModel) {
  val baseUri="http://example.com/base/"
  type GeneratedTriple=(Resource,Property,Object,RDFDatatype)
  private def addTriple(m:Model,subj:Resource,p:Property,obj:(Object,RDFDatatype),poMap:PredicateObjectMap)={
    val (ob,datatype)=obj
    val oMap=poMap.objectMap
	if (ob!=null)
	  if (ob.isInstanceOf[Resource])
		m.add(subj,p,ob.asInstanceOf[Resource])
	  else if (oMap.dtype!=null && datatype!=null)		          
	    m.add(subj,p,ob.toString,datatype)
	  else if (oMap.column!=null && oMap.termType==IRIType)
		m.add(subj,p,m.createResource(ob.toString))	   
	  else if (oMap.column!=null && datatype!=null && datatype==XSDDatatype.XSDstring)
		m.add(subj,p,ob.toString,oMap.language)	   
	  else if (oMap.column!=null && datatype!=null && datatype!=XSDDatatype.XSDstring)
		m.add(subj,p,ob.toString,datatype)	   
	  else if (oMap.template!=null && oMap.termType==LiteralType)
		m.add(subj,p,ob.toString,oMap.language)	   
	  else if (oMap.constant!=null && oMap.termType==LiteralType)
		m.add(subj,p,ob.toString,oMap.language)	   
	  else if (oMap.constant!=null )
		m.add(subj,p,ob.toString,oMap.language)	   
	  else
	    m.add(subj,p,m.createResource(ob.toString))
  }
  
  def generate={
    val m = ModelFactory.createDefaultModel
	val ds = DatasetFactory.create(m) 
    if (model.tMaps.isEmpty)
      throw new Exception("No valid R2RML mappings in the provided document.")
    val queries=model.tMaps.foreach{tMap=>
      val q=new RdbQueryGenerator(tMap,model).query
      println("query "+q)
      
      val res=try relational.query(q)
      catch {case ex:SQLSyntaxErrorException=>
        throw new RelationalQueryException("Invalid query: "+ex.getMessage,ex)} 
      
      val tgen=new TripleGenerator(ds,tMap,baseUri)
      while (res.next){        
        val subj = tgen.genSubject(res)
        val tMapModel = tgen.graph(res,tMap.subjectMap.graphMap)
          
        if (subj!=null){
				
          if (tgen.genRdfType!=null)
            tMapModel.add(subj,RDF.typeProp,tgen.genRdfType)
												
		  tMap.poMaps.foreach{prop=>
		    val parentTmap=if (prop.refObjectMap!=null) 
		      model.triplesMaps(prop.refObjectMap.parentTriplesMap)
		      else null
		    val propIns = POGenerator(ds,prop,parentTmap,baseUri)
		    val po=propIns.generate(null,res)
		    println("data"+po)
		    addTriple(tMapModel,subj,propIns.genPredicate,po,prop)
		    if (prop.graphMap!=null){
		      val poModel=tgen.graph(res,prop.graphMap)
		      addTriple(poModel,subj,propIns.genPredicate,po,prop)
		    }
		  }
        }
        
      }
    }
    ds
  }
}