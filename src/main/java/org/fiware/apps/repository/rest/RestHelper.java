package org.fiware.apps.repository.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.fiware.apps.repository.model.Resource;
import org.fiware.apps.repository.model.ResourceCollection;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DCTerms;

public class RestHelper {

	static HashMap<String, String> typeMap;
	static	{
		typeMap = new HashMap<String, String>();
		typeMap.put("application/json","application/json");
		typeMap.put("application/rdf+xml","RDF/XML");
		typeMap.put("text/turtle","TURTLE");
		typeMap.put("text/n3","N-TRIPLE");
		typeMap.put("text/html","text/html");
		typeMap.put("application/x-ms-application","text/html");
		typeMap.put("text/plain","text/plain");
		typeMap.put("application/xml","application/xml");
	}



	public static Response multiFormatResponse(Object obj, Class clazz, String type, UriInfo uriInfo) throws JAXBException{

		type=type.toLowerCase();
		String[] acceptedTypes = type.split(",");
		for(int i =0; i < acceptedTypes.length ; i++){
			if(typeMap.containsKey(acceptedTypes[i])){

				if(type.startsWith("application/json")){
					return jsonResponse(obj, clazz);
				}

				if(type.startsWith("application/rdf+xml")){
					return rdfResponse(obj, clazz, uriInfo, acceptedTypes[i]);
				}

				if(type.startsWith("text/turtle")){
					return rdfResponse(obj, clazz, uriInfo, acceptedTypes[i]);
				}

				if(type.startsWith("text/n3")){
					return rdfResponse(obj, clazz, uriInfo, acceptedTypes[i]);
				}

				if(type.startsWith("text/html")){
					//return rdfResponse(obj, clazz, uriInfo, "application/rdf+xml");
					return htmlResponse(obj, clazz);		
				}

				if(type.startsWith("application/x-ms-application")){
					return htmlResponse(obj, clazz);
				}

				if(type.startsWith("text/plain")){
					return textResponse(obj, clazz);
				}

			}
		}

		return xmlResponse(obj, clazz);

	}



	public static Response xmlResponse (Object obj, Class clazz) throws JAXBException{
		JAXBContext ctx;
		ctx = JAXBContext.newInstance(clazz);
		StringWriter writer = new StringWriter();
		ctx.createMarshaller().marshal(obj, writer);
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Response.status(200).type("application/xml").entity(writer.toString()).build();						

	}

	public static Response jsonResponse (Object obj, Class clazz) throws JAXBException{
		StringWriter writer = new StringWriter();		
		try {
			ObjectMapper mapper = new ObjectMapper();

			MappingJsonFactory jsonFactory = new MappingJsonFactory();
			JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);

			mapper.writeValue(jsonGenerator, obj);				
			writer.close();		

		} catch (IOException e) {			
			e.printStackTrace();
		}

		return Response.status(200).type("application/json").entity(writer.toString()).build();
	}

	public static Response rdfResponse (Object obj, Class clazz, UriInfo uriInfo, String type) throws JAXBException{

		String content="";	

		if (clazz.equals(ResourceCollection.class)){
			ResourceCollection col = (ResourceCollection) obj;

			String collectionUri    = uriInfo.getAbsolutePath().toASCIIString();

			// create an empty Model
			Model model = ModelFactory.createDefaultModel();

			String date = "not specified";
			if(col.getCreationDate()!=null){
				date=col.getCreationDate().toString();
			}

			com.hp.hpl.jena.rdf.model.Resource res	= model.createResource(collectionUri).addProperty(DCTerms.date, date);

			Bag innerC = model.createBag(uriInfo.getAbsolutePath().toASCIIString()+"#collections/");
		
	
			for(ResourceCollection innerCol : col.getCollections()){	
		
				Model innerColMod = ModelFactory.createDefaultModel();

				String innerDate = "not specified";
				if(innerCol.getCreationDate()!=null){
					innerDate=innerCol.getCreationDate().toString();
				}
				innerColMod.createResource(uriInfo.getBaseUri().toASCIIString()+innerCol.getId())
				.addProperty(DCTerms.date, innerDate);

				StmtIterator iter = innerColMod.listStatements();			
				while (iter.hasNext()) {
					innerC.add(iter.nextStatement().getSubject());
				}
	
						
			}


			Bag innerResBag = model.createBag(uriInfo.getAbsolutePath().toASCIIString()+"#resources/");
		
			for(Resource innerRes : col.getResources()){		
				Model innerColMod = ModelFactory.createDefaultModel();

				String innerDate = "not specified";
				if(innerRes.getCreationDate()!=null){
					innerDate=innerRes.getCreationDate().toString();
				}
				innerColMod.createResource(uriInfo.getBaseUri().toASCIIString()+innerRes.getId())
				.addProperty(DCTerms.date, innerDate);

				StmtIterator iter = innerColMod.listStatements();			
				while (iter.hasNext()) {
					innerResBag.add(iter.nextStatement().getSubject());
				}


			}



			Writer writer = new StringWriter();			
			model.write(writer, typeMap.get(type));
			return Response.status(200).type(type).entity(writer.toString()).build();	


		}	

		if (clazz.equals(Resource.class)){						
			
			Resource col = (Resource) obj;

			String collectionUri    = uriInfo.getAbsolutePath().toASCIIString();

			// create an empty Model
			Model model = ModelFactory.createDefaultModel();

			String date = "not specified";
			if(col.getCreationDate()!=null){
				date=col.getCreationDate().toString();
			}
			
			String modified = "not specified";
			if(col.getModificationDate()!=null){
				date=col.getModificationDate().toString();
			}
			
			com.hp.hpl.jena.rdf.model.Resource res
			= model.createResource(collectionUri)
			.addProperty(DCTerms.date, date)
			.addProperty(DCTerms.modified, modified)
			.addProperty(DCTerms.title, col.getContentFileName())
			.addProperty(DCTerms.format, col.getContentMimeType());
				
			
			Writer writer = new StringWriter();			
			model.write(writer, typeMap.get(type));
			return Response.status(200).type(type).entity(writer.toString()).build();			
			
			

		}

		content+="No Representation for Class "+clazz.getCanonicalName()+" found.";			
		return Response.status(500).type("text/html").entity(content).build();				
	}

	public static Response htmlResponse (Object obj, Class clazz) throws JAXBException{

		StringBuilder content = new StringBuilder();


		if (clazz.equals(ResourceCollection.class)){
			ResourceCollection col = (ResourceCollection) obj;

			content.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"><html><head><title>Collection: "+col.getId()+"</title>");
			content.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"/FiwareRepository/style/style.css\"></head><body>");

			content.append("<h1>Collection: "+col.getId()+"</h1>"); 
			content.append("Creation Date: "+col.getCreationDate());
			content.append("<h2>Collections:</h2>");
			content.append("<table class=\"tab\">");
			content.append("<th>Collection Id</th><th>Creation Date</th>");
			for(ResourceCollection innerCol : col.getCollections()){
				content.append("<tr>");
				content.append("<td><a href=\"/FiwareRepository/v1/"+innerCol.getId()+"\">"+innerCol.getId()+"</a></td>");  
				content.append("<td>"+innerCol.getCreationDate()+"</td>");
				content.append("</tr>");
			}
			content.append("</table><br />");			
			content.append("<h2>Resources:</h2>");
			content.append("<table class=\"tab\">");
			content.append("<th>Resource Id</th><th>Resource Meta Information</th><th>Creation Date</th><th>Modification Date</th><th>Filename</th><th>Mime Type</th>");
			for(Resource res : col.getResources()){
				content.append("<tr>");
				content.append("<td><a href=\"/FiwareRepository/v1/"+res.getId()+"\">"+res.getId()+"</a></td>"); 
				content.append("<td><a href=\"/FiwareRepository/v1/"+res.getId()+".meta\">Meta Information</a></td>"); 
				content.append("<td>"+res.getCreationDate()+"</td>"); 
				content.append("<td>"+res.getModificationDate()+"</td>"); 
				content.append("<td>"+res.getContentFileName()+"</td>");
				content.append("<td>"+res.getContentMimeType()+"</td>");
				content.append("</tr>");

			}
			content.append("</table><br />");
			content.append("</body></html>");
			return Response.status(200).type("text/html").entity(content.toString()).build();		
		}	

		if (clazz.equals(Resource.class)){
			Resource res = (Resource) obj;

			content.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"><html><head><title>Resource: "+res.getId()+"</title>");
			content.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"/FiwareRepository/style/style.css\"></head><body>");

			content.append("<h1>Resource: "+res.getId()+"</h1>"); 

			content.append("<table class=\"tab\">");
			content.append("<th>Resource Id</th><th>Creation Date</th><th>Modification Date</th><th>Filename</th><th>Mime Type</th>");

			content.append("<tr>");
			content.append("<td><a href=\"/FiwareRepository/v1/"+res.getId()+"\">"+res.getId()+"</a></td>"); 
			content.append("<td>"+res.getCreationDate()+"</td>"); 
			content.append("<td>"+res.getModificationDate()+"</td>"); 
			content.append("<td>"+res.getContentFileName()+"</td>");
			content.append("<td>"+res.getContentMimeType()+"</td>");
			content.append("</tr>");				

			content.append("</table><br />");
			content.append("</body></html>");
			return Response.status(200).type("text/html").entity(content.toString()).build();		
		}

		content.append("No Representation for Class "+clazz.getCanonicalName()+" found.");			
		return Response.status(500).type("text/html").entity(content.toString()).build();				

	}

	public static Response textResponse (Object obj, Class clazz) throws JAXBException{
		StringBuilder content = new StringBuilder();	

		if (clazz.equals(ResourceCollection.class)){
			ResourceCollection col = (ResourceCollection) obj;


			content.append("Collection: "+col.getId()+"\n"); 
			content.append("Creation Date: "+col.getCreationDate()+"\n\n");
			content.append("Collections:\n");	
			content.append("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
			content.append(String.format("%-3s %-30s %-3s %-33s %-3s" , "++", "Collection Id", "+", "Creation Date", "++"));		
			content.append("\n");
			content.append("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");			
			for(ResourceCollection innerCol : col.getCollections()){	
				content.append(String.format("%-3s %-30s %-3s %-33s %-3s" , "++", innerCol.getId(), "+", innerCol.getCreationDate(), "++"));						     	
				content.append("\n"); 		
			}
			content.append("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");


			content.append("\n\n");			
			content.append("Resources:\n");
			content.append("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
			content.append(String.format("%-3s %-30s %-3s %-30s %-3s %-30s %-3s %-20s %-3s %-22s %-3s" , "++", "Resource Id", "+", "Creation Date", "+", "Modification Date", "+", "Filename", "+", "Mime Type", "++"));	
			content.append("\n");
			content.append("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");
			for(Resource res : col.getResources()){
				content.append(String.format("%-3s %-30s %-3s %-30s %-3s %-30s %-3s %-20s %-3s %-22s %-3s" , "++", res.getId(), "+", res.getCreationDate(), "+", res.getModificationDate(), "+", res.getContentFileName(), "+", res.getContentMimeType(), "++"));	
				content.append("\n"); 	
			}
			content.append("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n");

			return Response.status(200).type("text/plain").entity(content).build();		
		}	

		if (clazz.equals(Resource.class)){
			Resource res = (Resource) obj;

			content.append("Resource Information:\n"); 

			content.append(String.format("%-23s %-15s" ,"Resource Id:" , res.getId()));		
			content.append("\n");

			content.append(String.format("%-23s %-15s" , "Creation Date:", res.getCreationDate()));		
			content.append("\n");

			content.append(String.format("%-23s %-15s" ,"Modification Date:" , res.getModificationDate()));		
			content.append("\n");

			content.append(String.format("%-23s %-35s" ,"Filename:" , res.getContentFileName()));		
			content.append("\n");

			content.append(String.format("%-23s %-35s" , "Mime Type:", res.getContentMimeType()));		
			content.append("\n");


			return Response.status(200).type("text/plain").entity(content.toString()).build();		
		}


		content.append("No Representation for Class "+clazz.getCanonicalName()+" found.");				
		return Response.status(500).type("text/plain").entity(content.toString()).build();						

	}


}
