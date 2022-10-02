
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters.FileConverter;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {




   
   public static String getToken() {
      return "c27a45b6e4330f94dec9fa4e99ed5eeb9cf248a4";
   }




  // TODO: CRIO_TASK_MODULE_REST_API
  //  Find out the closing price of each stock on the end_date and return the list
  //  of all symbols in ascending order by its close value on end date.

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  //    and deserialize the results in List<Candle>

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    
     List<TotalReturnsDto> dtos = new ArrayList<TotalReturnsDto>();
     File file = resolveFileFromResources(args[0]);
     ObjectMapper objectMapper = getObjectMapper();
     List<String> str = new ArrayList<>();
     RestTemplate restTemplate = new RestTemplate();

     
     
     LocalDate localDate = LocalDate.parse(args[1]);
     PortfolioTrade[] portfolioTrades = objectMapper.readValue(file, PortfolioTrade[].class);
     for (PortfolioTrade p : portfolioTrades) {
        TiingoCandle[] results = restTemplate.getForObject(
              prepareUrl(p, localDate, "c27a45b6e4330f94dec9fa4e99ed5eeb9cf248a4"),
              TiingoCandle[].class);
        if (results != null) {
           dtos.add(new TotalReturnsDto(p.getSymbol(), results[results.length - 1].getClose()));
        }

     }


     List<TotalReturnsDto> sorted = dtos;
     Collections.sort(sorted, TotalReturnsDto.closingComparator);
     for (TotalReturnsDto td : sorted) {
        str.add(td.getSymbol());
     }

     return str;
   



  }
  
 


  private static void printJsonObject(Object object) throws IOException {
     Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
     ObjectMapper mapper = new ObjectMapper();
     mapper.registerModule(new JavaTimeModule());
     logger.info(mapper.writeValueAsString(object));
  }

  private static ObjectMapper getObjectMapper() {
     ObjectMapper objectMapper = new ObjectMapper();
     objectMapper.registerModule(new JavaTimeModule());
     return objectMapper;
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
     return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI())
           .toFile();
  }

  private static String readFileAsString(String filename) throws URISyntaxException, IOException {
     return new String(Files.readAllBytes(resolveFileFromResources(filename).toPath()), "UTF-8");
  }
  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.










  // TODO:
  //  Ensure all tests are passing using below command
  //  ./gradlew test --tests ModuleThreeRefactorTest
  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
     
     return  candles.get(0).getOpen();
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
   
     return  candles.get(candles.size() - 1).getClose() ;
     
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) throws JsonMappingException, JsonProcessingException {
     
     RestTemplate restTemplate = new RestTemplate();
     String tiingoRestURL = prepareUrl(trade, endDate, token);
     TiingoCandle[] tiingoCandleArray =
           restTemplate.getForObject(tiingoRestURL, TiingoCandle[].class);
     return Arrays.stream(tiingoCandleArray).collect(Collectors.toList());
          
        
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
        throws IOException, URISyntaxException {
         
     List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);
     List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
     LocalDate localDate = LocalDate.parse(args[1]);
     for (PortfolioTrade portfolioTrade : portfolioTrades) {
        List<Candle> candles = fetchCandles(portfolioTrade, localDate, 
              "c27a45b6e4330f94dec9fa4e99ed5eeb9cf248a4");
        AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(localDate, portfolioTrade,
              getOpeningPriceOnStartDate(candles), getClosingPriceOnEndDate(candles));
        annualizedReturns.add(annualizedReturn);
     }
     return annualizedReturns.stream()
           .sorted((a1, a2) -> Double.compare(a2.getAnnualizedReturn(), a1.getAnnualizedReturn()))
           .collect(Collectors.toList());
    
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Return the populated list of AnnualizedReturn for all stocks.
  //  Annualized returns should be calculated in two steps:
  //   1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  //      1.1 Store the same as totalReturns
  //   2. Calculate extrapolated annualized returns by scaling the same in years span.
  //      The formula is:
  //      annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  //      2.1 Store the same as annualized_returns
  //  Test the same using below specified command. The build should be successful.

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
        PortfolioTrade trade, Double buyPrice, Double sellPrice) {
     double totalReturns = (sellPrice - buyPrice) / buyPrice;
     String symbol = trade.getSymbol();
     LocalDate startDate = trade.getPurchaseDate();
    double daysDifference = (double)ChronoUnit.DAYS.between(startDate, endDate);
     double totalNumYears = daysDifference / 365;
     double annualizedReturn = Math.pow((1 + totalReturns), (1/totalNumYears)) - 1;
      return new AnnualizedReturn(symbol,annualizedReturn,totalReturns);
  }



// TODO:
  //  After refactor, make sure that the tests pass by using these two commands
  //  ./gradlew test --tests PortfolioManagerApplicationTest.readTradesFromJson
  //  ./gradlew test --tests PortfolioManagerApplicationTest.mainReadFile
  public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
    
     File file = resolveFileFromResources(filename);
     ObjectMapper om = getObjectMapper();
     PortfolioTrade[] portfolioTrades = om.readValue(
           file, PortfolioTrade[].class);
     List<PortfolioTrade> list = new ArrayList<>();
     for (PortfolioTrade p : portfolioTrades) {
        list.add(p);
     }
     return list;
  }
  
  

  // TODO:
  //  Build the Url using given parameters and use this function in your code to cann the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
     return "https://api.tiingo.com/tiingo/daily/" + trade.getSymbol() + "/prices?startDate="
           + trade.getPurchaseDate() + "&endDate=" + endDate + "&token=" + token;
  }


  public static List<String> debugOutputs() {
     String valueOfArgument0 = "trades.json";
     String resultOfResolveFilePathArgs0 = "/qmoney/resources/main/trades.json";
     String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@7350471";
     String functionNameFromTestFileInStackTrace = "mainReadFile";
     String lineNumberFromTestFileInStackTrace = "29";


     return Arrays.asList(
           new String[] {valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
                 functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace});
  }


  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
     String file = args[0];
     String contents = readFileAsString(file);
     ObjectMapper objectMapper = getObjectMapper();
     PortfolioTrade[] portfolioTrades = objectMapper.readValue(contents, PortfolioTrade[].class);
     // multiple symbols
     return Stream.of(portfolioTrades).map(PortfolioTrade::getSymbol).collect(Collectors.toList());
  }




















  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
      //  String file = args[0];
      //  LocalDate endDate = LocalDate.parse(args[1]);
      //  String contents = readFileAsString(file);
      //  ObjectMapper objectMapper = getObjectMapper();
      //  return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);

       String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    String contents = readFileAsString(file);    
    ObjectMapper objectMapper = getObjectMapper();    
    PortfolioManager portfolioManager =
        PortfolioManagerFactory.getPortfolioManager(new RestTemplate());
    List<PortfolioTrade> portfolioTrades=objectMapper.readValue(contents,new TypeReference<List<PortfolioTrade>>() {});    
    return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
  }








  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());


    


    //printJsonObject(mainCalculateSingleReturn(args));



    printJsonObject(mainCalculateReturnsAfterRefactor(args));


  }
}

