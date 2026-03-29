package com.chtrembl.petstore.orderitemsreserver.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class HelloHandler {

    @FunctionName("hello")
    public HttpResponseMessage execute(
        @HttpTrigger(
            name = "request",
            methods = {HttpMethod.GET},
            authLevel = AuthorizationLevel.ANONYMOUS
        ) HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context) {
        context.getLogger().info("Hello Java HTTP trigger processed a request.");

        return request.createResponseBuilder(HttpStatus.ACCEPTED)
            .body("hi")
            .build();

    }
}
