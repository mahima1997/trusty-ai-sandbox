package com.redhat.developer.execution.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.developer.database.IEventStorage;
import com.redhat.developer.execution.responses.execution.ExecutionHeaderResponse;
import com.redhat.developer.execution.responses.execution.ExecutionResponse;
import com.redhat.developer.execution.storage.model.DMNEventModel;
import com.redhat.developer.execution.storage.model.DMNResultModel;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

@Path("/executions")
public class ExecutionsApi {

    @Inject
    IEventStorage storageService;

    @GET
    @APIResponses(value = {
            @APIResponse(description = "Returns the execution headers.", responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, implementation = ExecutionResponse.class))),
            @APIResponse(description = "Bad Request", responseCode = "400", content = @Content(mediaType = MediaType.TEXT_PLAIN))
    }
    )
    @Operation(summary = "Gets the execution headers.", description = "Gets the execution headers.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExecutions(
            @Parameter(
                    name = "from",
                    description = "Start datetime for the lookup. Date in the format \"yyyy-MM-dd'T'HH:mm:ss.SSS\"",
                    required = false,
                    schema = @Schema(implementation = String.class)
            ) @DefaultValue("yesterday") @QueryParam("from") String from,
            @Parameter(
                    name = "to",
                    description = "End datetime for the lookup. Date in the format \"yyyy-MM-dd'T'HH:mm:ss.SSS\"",
                    required = false,
                    schema = @Schema(implementation = String.class)
            ) @DefaultValue("now") @QueryParam("to") String to,
            @Parameter(
                    name = "limit",
                    description = "Maximum number of results to return.",
                    required = false,
                    schema = @Schema(implementation = Integer.class)
            ) @DefaultValue("100") @QueryParam("limit") int limit,
            @Parameter(
                    name = "offset",
                    description = "Offset for the pagination.",
                    required = false,
                    schema = @Schema(implementation = Integer.class)
            ) @DefaultValue("0") @QueryParam("offset") int offset) {

        if (from.equals("yesterday")) {
            from = java.time.LocalDateTime.now().minusDays(1).toString();
        }
        List<DMNResultModel> results;

        try {
            results = storageService.getDecisions(from, to);
        } catch (RuntimeException e) {
            return Response.status(400, String.format("Bad request: {}", e.getMessage())).build();
        }

        int totalResults = results.size();
        if (totalResults >= limit) {
            results.sort(Comparator.comparing(DMNResultModel::getExecutionDate));
            results = results.subList(offset, Math.min(results.size(), offset + limit));
        }

        List<ExecutionHeaderResponse> executionResponses = new ArrayList<>();
        results.forEach(x -> executionResponses.add(ExecutionHeaderResponse.fromDMNResultModel(x)));
        return Response.ok(new ExecutionResponse(totalResults, limit, offset, executionResponses)).build();
    }

    @GET
    @Path("/search")
    @APIResponses(value = {
            @APIResponse(description = "Returns the execution headers filtered by ID partial match.", responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, implementation = ExecutionResponse.class))),
            @APIResponse(description = "Bad Request", responseCode = "400", content = @Content(mediaType = MediaType.TEXT_PLAIN))
    }
    )
    @Operation(summary = "Gets  the execution headers filtered by ID partial match.", description = "Gets the execution headers filtered by ID partial match.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(
            @Parameter(
                name = "limit",
                description = "Maximum number of results to return.",
                required = false,
                schema = @Schema(implementation = Integer.class)
            ) @DefaultValue("100") @QueryParam("limit") int limit,
           @Parameter(
                   name = "offset",
                   description = "Offset for the pagination.",
                   required = false,
                   schema = @Schema(implementation = Integer.class)
           ) @DefaultValue("0") @QueryParam("offset") int offset,
            @Parameter(
                name = "id",
                description = "ID substring to match",
                required = true,
                schema = @Schema(implementation = String.class)
            ) @NotNull @QueryParam("id") String id){
        List<DMNResultModel> results = storageService.getEventsByMatchingId(id).stream().map(x -> x.data.result).collect(Collectors.toList());

        int totalResults = results.size();
        if (totalResults >= limit) {
            results.sort(Comparator.comparing(DMNResultModel::getExecutionDate));
            results = results.subList(offset, Math.min(results.size(), offset + limit));
        }

        List<ExecutionHeaderResponse> executionResponses = new ArrayList<>();
        results.forEach(x -> executionResponses.add(ExecutionHeaderResponse.fromDMNResultModel(x)));
        return Response.ok(new ExecutionResponse(totalResults, limit, offset, executionResponses)).build();
    }

}
