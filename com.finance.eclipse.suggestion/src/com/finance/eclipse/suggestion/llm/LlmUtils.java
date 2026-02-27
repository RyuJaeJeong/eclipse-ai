package com.finance.eclipse.suggestion.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.finance.eclipse.suggestion.AiActivator;
import com.finance.eclipse.suggestion.utils.JinjaUtils;
import com.finance.eclipse.suggestion.utils.Utils;

import mjson.Json;

public final class LlmUtils {
	
	// Field
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.version(Version.HTTP_1_1)
			.build();
	

	// Cons
	public LlmUtils() {

	}
	
	// Method
	public static CompletableFuture<LlmResponse> executeFillInTheMiddle(String prefix, String suffix) {
		return execute(LlmOption.createFillInMiddleModelOptionFromPreferences(), null, prefix, suffix);
	}
	
	public static CompletableFuture<LlmResponse> execute(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix){
		LlmProvider provider = llmModelOption.provider();
		switch(provider){
			case OPENAI:
				return executeOpenai(llmModelOption, systemPrompt, prompt, suffix);
			default: 
				throw new IllegalStateException("Illegal provider: " + provider);
		}
	}
	
	private static CompletableFuture<LlmResponse> executeOpenai(LlmOption llmModelOption, String systemPrompt, String prompt, String suffix){
		final boolean isFillInTheMiddle = suffix != null;
		final boolean isPseudoFim = true;
		
		// 한줄 주석 판단 
		String[] lines = prompt.split("\n");
		String lastLine = lines.length > 0 ? lines[lines.length - 1] : "";
		boolean isSingleLineComment = lastLine.contains("//");
		
		// 블록 주석(/* */) 판단 
		int lastOpen = prompt.lastIndexOf("/*");
		int lastClose = prompt.lastIndexOf("*/");
		boolean isMultiLineComment = lastOpen > lastClose;
		boolean isInsideComment = isSingleLineComment || isMultiLineComment;
		System.out.println("##### isInsideComment: " + isInsideComment);
		final String urlString = "https://api.openai.com";
		final String openAiApiKey = "";
		final boolean multilineEnabled = true;
		final Json json = Json.object();
		json.set("model", llmModelOption.modelKey());
		json.set("temperature", 1);
		if(isFillInTheMiddle){
			final String fimTemplatePrompt = JinjaUtils.applyTemplate("<|fim_prefix|>{{prefix}}<|fim_suffix|>{{suffix}}<|fim_middle|>", Map.ofEntries(Map.entry("prefix", prompt), Map.entry("suffix", suffix)));
			if(!isPseudoFim){
				json.set("prompt", fimTemplatePrompt);
				json.set("max_tokens", 256);
				json.set("stop", createStop(multilineEnabled));
			}else {
				String pseudoFimSystemPrompt = getPseduoFIMSystemPrompt();
				json.set("messages", createMessages(pseudoFimSystemPrompt, fimTemplatePrompt));
			}
		}else{
			json.set("messages", createMessages(systemPrompt, prompt));
		}
		final String path = isFillInTheMiddle && !isPseudoFim ? "/v1/completions" : "/v1/chat/completions";
		final URI uri = URI.create(Utils.joinUriParts(List.of(urlString, path)));
		final long beforeTimestamp = System.currentTimeMillis();
		final HttpRequest request = HttpRequest.newBuilder()
				.uri(uri)
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("Authorization", "Bearer " + openAiApiKey)
				.timeout(Duration.ofMinutes(5))
				.POST(HttpRequest.BodyPublishers.ofString(json.toString()))
				.build();
		return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
						  .thenApply(response -> {
							  final Duration duration = Duration.ofMillis(System.currentTimeMillis() - beforeTimestamp);
							  if(response.statusCode() == 200) {
								  final String responseBody = response.body();
								  final Json responseJson = Json.read(responseBody);
								  final String content = isFillInTheMiddle && !isPseudoFim? responseJson.at("choices").at(0).at("text").asString(): responseJson.at("choices").at(0).at("message").at("content").asString();
								  final int inputTokens = responseJson.at("usage").at("prompt_tokens").asInteger();
								  final int outputTokens = responseJson.at("usage").at("completion_tokens").asInteger();
								  return new LlmResponse(llmModelOption, content, responseBody, inputTokens, outputTokens, duration, false);
							  }else {
								  AiActivator.log().log(new Status(IStatus.WARNING, AiActivator.PLUGIN_ID, String.format("Error: %s (%s)", response.body(), response.statusCode())));
								  return new LlmResponse(llmModelOption, "", response.body(), 0, 0, duration, true);
							  }
						  });
	}
	
	public static String getPseduoFIMSystemPrompt() {
		return """
				You are an expert code completion AI.
				Complete the code.
				- The user will provide a code snippet formatted as a "Fill in the Middle" (FIM) request with <|fim_prefix|>, <|fim_suffix|>, and <|fim_middle|> tags.
				- You must strictly complete the code, starting immediately after the <|fim_middle|> tag, and return **ONLY** the generated completion code, without any surrounding explanation or text.
				- Do not include the prefix or suffix in your response.
				- Do not repeat any of the provided context in the response.
				- Partial code snippets are expected.
				- Provide the completion that fills in the missing code.

				## Example prompts and responses

				**Example 1:**
				```
				<|fim_prefix|># Current edit location: [path];

				public class Main {

					public static void main(String[] args) {
						// TODO: add a for loop count from 1 to 10
						for (<|fim_suffix|>
					}
				}
				<|fim_middle|>
				```
				Correct response:
				```
				int i = 1; i <= 10; i++) {
							System.out.println(i);
						}
				```

				**Example 2:**
				```
				<|fim_prefix|># Current edit location: [path];

				public class Main {

					public static void main(String[] args) {
						// TODO: add a for loop count from 1 to 10
						for(<|fim_suffix|>)
					}
				}
				<|fim_middle|>
				```
				Correct response:
				```int i = 1; i <= 10; i++```

				**Example 3:**
				```
				<|fim_prefix|># Current edit location: [path];
				public class Main {

					public static void main(String[] args) {
						int j = 100;
						while(j<|fim_suffix|>
					}
				}
				<|fim_middle|>
				```
				Correct response:
				```
				> 0) {
					System.out.println(j);
					j--;
				}
				```
				**Example 4:**
				```
				<|fim_prefix|># Current edit location: [path];

				public class Main {

					public static void main(String[] args) {
						int j = 100;
						while(j<|fim_suffix|>)
					}
				}
				<|fim_middle|>
				```
				Correct response:
				is:
				```	> 0```

				**Example 5:**
				```
				<|fim_prefix|># Current edit location: [path];

				public class Main {

					public static void main(String[] args) {
						String title = "A FIM example.";
				   		System.out
					}
				}
				<|fim_middle|>
				```
				Correct response:
				```.println(title);```

				## Guidelines ##
				- Use the correct variables based on the context
				- Focus on short, high confidence completions
				- Do not generate extraneous code that does not logically fill in the completion
				- When the completion is combined with the context code, it should be logically and syntactically correct and compilable
				- Pay attention to opening and closing characters such as braces and parentheses
				""".trim();
	}
	
	private static Json createStop(final boolean multilineEnabled) {
		return Json.array()
				.add(multilineEnabled ? "\n\n" : "\n")
				.add(multilineEnabled ? "\r\n\r\n" : "\r\n");
	}

	private static Json createMessages(String systemPrompt, String prompt) {
		return Json.array()
				.add(Json.object()
						.set("role", "system")
						.set("content", systemPrompt))
				.add(Json.object()
						.set("role", "user")
						.set("content", prompt));
	}
	
}
