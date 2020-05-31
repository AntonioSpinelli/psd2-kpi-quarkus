package com.ibm.psd2kpiq.bean;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.psd2kpiq.dao.Psd2KeyPerformanceIndicatorLoggerDao;
import com.ibm.psd2kpiq.dto.BaseResponse;
import com.ibm.psd2kpiq.dto.KpiLogRequest;
import com.ibm.psd2kpiq.dto.MovimentiCcRequest;
import com.ibm.psd2kpiq.dto.MovimentiCcResponse;
import com.ibm.psd2kpiq.util.KpiLogUtilities;

import io.vertx.axle.pgclient.PgPool;
import io.vertx.axle.sqlclient.Tuple;


@Path("/api/v1/kpipsd2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Psd2KpiResource {

	@Inject
    PgPool client;
	
	@Inject
    @ConfigProperty(name = "kpipsd2.schema.create", defaultValue = "false") 
    boolean schemaCreate;
	
    @PostConstruct
    void config() {
    	if(schemaCreate)
          initdb();
    }
    
    private void initdb() {
	  	client.query("CREATE SCHEMA PSD2")
	    .thenCompose(r -> client.query("CREATE TABLE PSD2.TB_NW_PSD2_KPI_LOG (\n" + 
	    		"COD_AZ_OPERANTE SMALLINT NOT NULL,\n" + 
	    		"COD_CANALE SMALLINT NOT NULL,\n" + 
	    		"IND_T_OPERAZIONE SMALLINT NOT NULL,\n" + 
	    		"TS_INIZIO TIMESTAMP NOT NULL,\n" + 
	    		"TS_FINE TIMESTAMP NOT NULL,\n" + 
	    		"DESCRIZIONE VARCHAR(256) NOT NULL,\n" + 
	    		"STEP SMALLINT NOT NULL,\n" + 
	    		"ESITO SMALLINT NOT NULL,\n" + 
	    		"REQ_LAYER CHAR(10),\n" + 
	    		"	PRIMARY KEY (COD_AZ_OPERANTE, COD_CANALE, IND_T_OPERAZIONE, TS_INIZIO, TS_FINE)\n" + 
	    		")"))
        .toCompletableFuture()
        .join();
    }
	
	@GET
	@Path("/test")
	public Response kpipsd2(){

		  MovimentiCcRequest parameters = new MovimentiCcRequest();
			  parameters.setAbi(9999);
			  parameters.setCanale(31);
			  parameters.setCanaleMovimento(30);
			  parameters.setCategoriaMovimento(6);
			  parameters.setDataA(Calendar.getInstance());
			  parameters.setDataDa(Calendar.getInstance());
			  parameters.setGuid(UUID.randomUUID().toString());
			  parameters.setNumeroMovimenti(2);
			  parameters.setTipologia(20);
	  
		  MovimentiCcResponse responseMov = new MovimentiCcResponse();
			  responseMov.setDescrizione("Test");
			  responseMov.setEsito(0);
			  responseMov.setGuid(UUID.randomUUID().toString());
	  	  
		  KpiLogRequest request = KpiLogUtilities.getKpiLogTPRequest(parameters, responseMov, Calendar.getInstance(), (short)1, 10, "Test");
		  BaseResponse baseResponse = inserNewLogImpl(request);	  
		  
		  return Response.ok(baseResponse).build();
	 }
	
    @POST
    @Path("/insertNewLog")
    @Produces(MediaType.TEXT_PLAIN)
    public Response insertNewLog(KpiLogRequest request){
  	  
    	BaseResponse response = inserNewLogImpl(request);
  	  
  	  return Response.ok(response).build();
    }
    
    
    private BaseResponse inserNewLogImpl(KpiLogRequest request) {
  	  
  	  BaseResponse response = new BaseResponse();

  	  try {
  		  Psd2KeyPerformanceIndicatorLoggerDao stm = new Psd2KeyPerformanceIndicatorLoggerDao();
  		  String insertQuery = stm.getInsertQuery(request.getReqLayer());

  		  List<Object> elems = new ArrayList<Object>();
  		  Tuple values = null;
  		  
  		  if (request.getReqLayer() == null) {
	  		  elems.add(request.getAbi());
	  		  elems.add(request.getCanale());
	  		  elems.add(request.getIndTOperazione());
		  	  elems.add(request.getTsInizio().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
		  	  elems.add(request.getTsFine().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
	  		  elems.add(request.getDescrizione());
	  		  elems.add(request.getStep());
	  		  elems.add(request.getEsito());
  		  
	  		  values = Tuple.tuple(elems);
    		} else {
    			if(request.getReqLayer().length() > Psd2KeyPerformanceIndicatorLoggerDao.COLUMN_REQ_LAYER_MAX_CHARS) {
	    			request.setReqLayer(request.getReqLayer().substring(0, Psd2KeyPerformanceIndicatorLoggerDao.COLUMN_REQ_LAYER_MAX_CHARS));
	    		}
		  		elems.add(request.getAbi());
		  		elems.add(request.getCanale());
		  		elems.add(request.getIndTOperazione());
		  		elems.add(request.getTsInizio().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
		  		elems.add(request.getTsFine().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
		  		elems.add(request.getDescrizione());
		  		elems.add(request.getStep());
		  		elems.add(request.getEsito());
		  		elems.add(request.getReqLayer());
	  		  
		  		values = Tuple.tuple(elems);
    		}
  		    		 
  		  		CompletionStage<BaseResponse> cs = client
				  .preparedQuery(insertQuery,values)
				  .thenApply(pgRowSet -> {
					boolean esito;
	          		if(pgRowSet.rowCount() == 1) {
	          			System.out.println("Servizio eseguito correttamente");
	          			esito = true;
	          			return esito;
	          		}
	          		else {
	          			System.out.println("Servizio in errore");
	          			esito = false;
	          			return esito;
	          		}
	          		})
				  .thenApply(esito -> {
	          		if(esito) {
	          			response.setDescrizione("Servizio eseguito correttamente");
	    		  	  	response.setEsito(0);
	    		  	 	response.setGuid(request.getGuid());
	          			return response;
	          		}
	          		else {
	          			response.setDescrizione("Servizio in errore");
	          			response.setEsito(2);
	          			response.setGuid(request.getGuid());
	          			return response;
	          		}
	          		});
  		  	
  		  	return cs.toCompletableFuture().get();
  			  
  		  			  
  	  } catch (Exception e) {
  			e.printStackTrace();
  	  }
	return response;
       	  
    }
}