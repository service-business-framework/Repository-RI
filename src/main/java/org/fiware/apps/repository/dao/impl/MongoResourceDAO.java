package org.fiware.apps.repository.dao.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.fiware.apps.repository.dao.CollectionDAO;
import org.fiware.apps.repository.dao.DAOFactory;
import org.fiware.apps.repository.dao.MongoDAOFactory;
import org.fiware.apps.repository.dao.ResourceDAO;
import org.fiware.apps.repository.exceptions.db.DatasourceException;
import org.fiware.apps.repository.exceptions.db.SameIdException;
import org.fiware.apps.repository.model.Resource;
import org.fiware.apps.repository.model.ResourceCollection;
import org.fiware.apps.repository.model.ResourceFilter;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.ObjectId;

public class MongoResourceDAO implements ResourceDAO{

	public static final String MONGO_COLL_NAME = "Resource";

	private DB db;
	private DBCollection mongoCollection;

	DAOFactory mongoFactory = DAOFactory.getDAOFactory(DAOFactory.MONGO);
	CollectionDAO collectionDAO = mongoFactory.getCollectionDAO();

	public MongoResourceDAO(){
		db = MongoDAOFactory.createConnection();
		mongoCollection = db.getCollection(MONGO_COLL_NAME);
	}

	
	@Override
	public List<Resource> getResources(String id) throws DatasourceException {
		return getResources (id,  new ResourceFilter (0, 0, ""));
		
	}

	
	@Override
	public Resource getResource(String id) throws DatasourceException{
		Resource r = new Resource();
		db.requestStart();
		DBObject obj =null;
		try{		

			BasicDBObject query = new BasicDBObject();
			query.put("id", id);		
			obj = mongoCollection.findOne(query);			

		}catch (Exception e){			
			db.requestDone();	
			throw new DatasourceException("Error parsing " + r.getId() + " " + e.getMessage(), Resource.class );		
		}

		if(obj == null){				
			return null;	
		}

		r.setId(obj.get("id").toString());
		r.setName(obj.get("name").toString());
		r.setCreator(obj.get("creator").toString());
		r.setContentUrl(obj.get("contentUrl").toString());			
		r.setContentMimeType(obj.get("contentMimeType").toString());
		r.setContentFileName(obj.get("contentFileName").toString());
		
		if(obj.get("creationDate")!=null){
			r.setCreationDate((Date) obj.get("creationDate"));
		}
		if(obj.get("modificationDate")!=null){
			r.setModificationDate((Date) obj.get("modificationDate"));
		}		


		db.requestDone();
		return r;
	}
	
	
	@Override
	public Resource getResourceContent(String id) throws DatasourceException{
		Resource r = new Resource();
		db.requestStart();
		DBObject obj =null;
		try{		

			BasicDBObject query = new BasicDBObject();
			query.put("id", id);		
			obj = mongoCollection.findOne(query);			

		}catch (Exception e){			
			db.requestDone();	
			throw new DatasourceException("Error parsing " + r.getId() + " " + e.getMessage(), Resource.class );		
		}

		if(obj == null){				
			return null;	
		}

		r.setId(obj.get("id").toString());
		r.setName(obj.get("name").toString());
		r.setCreator(obj.get("creator").toString());
		r.setContentUrl(obj.get("contentUrl").toString());			
		r.setContentMimeType(obj.get("contentMimeType").toString());
		r.setContentFileName(obj.get("contentFileName").toString());
		r.setContent((byte[]) obj.get("content"));
		
		if(obj.get("creationDate")!=null){
			r.setCreationDate((Date) obj.get("creationDate"));
		}
		if(obj.get("modificationDate")!=null){
			r.setModificationDate((Date) obj.get("modificationDate"));
		}		

		db.requestDone();
		return r;
	}

	public Boolean isResource (String id) throws DatasourceException{		

		db.requestStart();
		DBObject obj =null;
		try{		

			BasicDBObject query = new BasicDBObject();
			query.put("id", id);					
			obj = mongoCollection.findOne(query);	

		}catch (Exception e){			
			db.requestDone();	
			throw new DatasourceException("Error parsing " + id + " " + e.getMessage(), Resource.class );		
		}
		db.requestDone();	
		if(obj == null){				
			return false;	
		}
		
		return true;
	}

	
	@Override
	public List<Resource> getResources(String path, ResourceFilter filter) throws DatasourceException {
		List <Resource> resources = new ArrayList<Resource>();
		db.requestStart();
		
		try{
		
		BasicDBObject query = filter.parseFilter();		
		
		Pattern p = Pattern.compile("^"+path+"/[a-zA-Z0-9_\\.\\-\\+]*$");	
		
		query.put("id", p);

		List <DBObject> objs = mongoCollection.find(query).skip(filter.getOffset()).limit(filter.getLimit()).toArray();

		for(DBObject obj : objs){
			
			Resource res = new Resource();
			res.setId(obj.get("id").toString());
			res.setName(obj.get("name").toString());
			res.setCreator(obj.get("creator").toString());
			res.setContentUrl(obj.get("contentUrl").toString());
			res.setContentMimeType(obj.get("contentMimeType").toString());
			res.setContentFileName(obj.get("contentFileName").toString());
			
			if(obj.get("creationDate")!=null){
				res.setCreationDate((Date) obj.get("creationDate"));
			}
			if(obj.get("modificationDate")!=null){
				res.setModificationDate((Date) obj.get("modificationDate"));
			}			
			
			resources.add(res);
		}
		}
		catch (Exception e){
			db.requestDone();
			e.printStackTrace();
			throw new DatasourceException(e.getMessage(), Resource.class );	
		}
		db.requestDone();
		return resources;
	}
	
	
	
	public Resource insertResource(Resource r)throws DatasourceException, SameIdException{

		if (findResource(r.getId()) != null){
			throw new SameIdException(r.getId(), Resource.class);
		}
		
		if(collectionDAO.getCollection(r.getId().substring(0, r.getId().lastIndexOf("/")))==null){
			ResourceCollection col = new ResourceCollection();
			col.setCreator(r.getCreator());
			col.setId(r.getId().substring(0, r.getId().lastIndexOf("/")));
			collectionDAO.insertCollection(col);
		}
	

		try{
			db.requestStart();
			
			BasicDBObject obj = new BasicDBObject();

			obj.put("id", r.getId());
			obj.put("name", r.getName());
			obj.put("creator", r.getCreator());
			obj.put("contentUrl", r.getContentUrl());    	
			obj.put("contentMimeType", r.getContentMimeType()); 
			obj.put("contentFileName", r.getContentFileName()); 	
			if(r.getCreationDate()!=null){
				obj.put("creationDate", r.getCreationDate()); 
			}else{
				obj.put("creationDate", new Date()); 
			}
			obj.put("modificationDate", new Date()); 	
						
			mongoCollection.insert(obj);
			db.requestDone();
			return r;
		}catch (Exception e){
			e.printStackTrace();
			db.requestDone();			
			throw new DatasourceException("Error parsing " + r.getId() + " " + e.getMessage(), Resource.class );	
		}
		
	}
	
	
	@Override
	public Resource findResource(String id) throws DatasourceException {
		Resource r = new Resource();
		db.requestStart();
		DBObject obj =null;
		try{		

			BasicDBObject query = new BasicDBObject();
			query.put("id", id);			
			obj = mongoCollection.findOne(query);			

		}catch (Exception e){			
			db.requestDone();	
			throw new DatasourceException("Error parsing " + r.getId() + " " + e.getMessage(), Resource.class );		
		}

		if(obj == null){				
			return null;	
		}

		r.setId(obj.get("id").toString());
		r.setName(obj.get("name").toString());
		r.setCreator(obj.get("creator").toString());
		r.setContentUrl(obj.get("contentUrl").toString());
		r.setContentMimeType(obj.get("contentMimeType").toString());
		r.setContentFileName(obj.get("contentFileName").toString());
		
		if(obj.get("creationDate")!=null){
			r.setCreationDate((Date) obj.get("creationDate"));
		}
		if(obj.get("modificationDate")!=null){
			r.setModificationDate((Date) obj.get("modificationDate"));
		}	


		db.requestDone();
		return r;
	}
	
	public Boolean updateResource(String path, Resource r) throws DatasourceException{
		db.requestStart();
		BasicDBObject query = new BasicDBObject();
		query.put("id", path);	
		DBObject obj = mongoCollection.findOne(query);	

		if(obj==null){
			db.requestDone();
			return false;		
		}

		if(collectionDAO.getCollection(r.getId().substring(0, r.getId().lastIndexOf("/")))==null){
			ResourceCollection col = new ResourceCollection();
			col.setCreator(r.getCreator());
			col.setId(r.getId().substring(0, r.getId().lastIndexOf("/")));
			try {
				collectionDAO.insertCollection(col);
			} catch (SameIdException e) {
				// should never happen
			}
		}
		
		obj.put("id", r.getId());
		obj.put("name", r.getName());
		obj.put("creator", r.getCreator());
		obj.put("contentUrl", r.getContentUrl()); 
		obj.put("contentMimeType", r.getContentMimeType()); 
		obj.put("contentFileName", r.getContentFileName()); 
		if(r.getCreationDate()!=null){
			obj.put("creationDate", r.getCreationDate()); 
		}else{
			obj.put("creationDate", new Date()); 
		}
		obj.put("modificationDate", new Date());	
		String internalId = obj.get("_id").toString();

		try{
			mongoCollection.update(new BasicDBObject().append("_id", new ObjectId(internalId)), obj, false,false);
			db.requestDone();
			return true;

		}catch (IllegalArgumentException e){
			db.requestDone();	
			throw new DatasourceException("Error updating Resource with ID " + r.getId() + " " + e.getMessage(), Resource.class );	
		}
	}
	
	public Boolean updateResourceContent(Resource r) throws DatasourceException{
		db.requestStart();
		try{
			BasicDBObject query = new BasicDBObject();
			query.put("id", r.getId());		
			DBObject obj = mongoCollection.findOne(query);			

			obj.put("content", r.getContent());
			obj.put("contentMimeType", r.getContentMimeType());
			obj.put("contentFileName", r.getContentFileName()); 
			if(r.getCreationDate()!=null){
				obj.put("creationDate", r.getCreationDate()); 
			}else{
				obj.put("creationDate", new Date()); 
			}
			obj.put("modificationDate", new Date()); 	
			

			String internalId = obj.get("_id").toString();

			mongoCollection.update(new BasicDBObject().append("_id", new ObjectId(internalId)), obj, false,false);		

		}catch (IllegalArgumentException e){
			db.requestDone();	
			throw new DatasourceException("Error updating Resource with ID " + r.getId() + " " + e.getMessage(), Resource.class );	
		}

		db.requestDone();
		return true;
	}
	
	@Override
	public Boolean deleteResource(String id) throws DatasourceException {		
		
		try{
			db.requestStart();		
			BasicDBObject query = new BasicDBObject();
			query.put("id", id);					
			DBObject obj = mongoCollection.findOne(query);	
			if(obj==null){
				db.requestDone();
				return false;		
			}
			mongoCollection.remove(obj);
			db.requestDone();
			return true;

		}catch (IllegalArgumentException e){
			db.requestDone();	
			throw new DatasourceException("Error deleting Collection with ID " + id + " " + e.getMessage(), Resource.class );	
		}

	}
	
	
}
