package tw.com.ehanlin.mde.dsl;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import tw.com.ehanlin.mde.dsl.action.*;
import tw.com.ehanlin.mde.util.EmptyObject;
import tw.com.ehanlin.mde.util.SpliceStringReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DslParser {

    public static DslParser instance = new DslParser();


    public Dsl parse(String dsl) {
        SpliceStringReader reader = new SpliceStringReader(dsl);
        SpliceStringReader.Matcher matcher = reader.splice(rootSymbols);
        if(matcher.finish()){
            return EmptyObject.Dsl;
        }

        List<Action> actions = new ArrayList();
        do{
            switch(matcher.match()){
                case "@" :
                    Action action = parseAction(reader);
                    if(action != null){
                        actions.add(action);
                    }
                    break;
                case "{" :
                    Dsl result = parseContent(reader);
                    result.appendAction(actions);
                    return result;
            }
        }while(!((matcher = reader.splice(rootSymbols)).finish()));

        return null;
    }




    private List<String> rootSymbols = Arrays.asList("@", "{");
    private List<String> propertySymbols = Arrays.asList("@", "{", "}");
    private List<String> actionScopeSymbols = Arrays.asList("(", "<", "[");
    private List<String> actionInfoSymbols = Arrays.asList("=", ",", "[", "{", ")", ">", "]");
    private List<String> mongoSymbols = Arrays.asList(":", ",", "{", "[", "]", "}");
    private Pattern stringPattern = Pattern.compile("^(?:'(.*)'|\"(.*)\")$");
    private Pattern atPattern = Pattern.compile("^@.*");
    private Pattern longPattern = Pattern.compile("^[+-]?\\d+$");
    private Pattern doublePattern = Pattern.compile("^[+-]?\\d*\\.\\d+$");
    private Pattern booleanPattern = Pattern.compile("^(true|false)$", Pattern.CASE_INSENSITIVE);


    private Object parseMongoContent(String content) {

        Matcher stringMatcher = stringPattern.matcher(content);
        if(stringMatcher.matches()){
            String result = stringMatcher.group(1);
            return (result != null) ? result : stringMatcher.group(2);
        }

        if(atPattern.matcher(content).matches()){
            return new At(content);
        }

        if(longPattern.matcher(content).matches()){
            return Long.parseLong(content);
        }

        if(doublePattern.matcher(content).matches()){
            return Double.parseDouble(content);
        }

        Matcher booleanMatcher = booleanPattern.matcher(content);
        if(booleanMatcher.matches()){
            return booleanMatcher.group(0).toLowerCase().equals("true");
        }

        return content;
    }

    private String parsePairSymbolsContent(String start, String end, SpliceStringReader reader){
        List<String> symbols = Arrays.asList(start, end);
        StringBuilder result = new StringBuilder(start);
        Integer count = 1;
        SpliceStringReader.Matcher matcher;
        while(!((matcher = reader.splice(symbols)).finish())){
            result.append(matcher.prefix());
            result.append(matcher.match());
            if(matcher.match().equals(start)){
                count += 1;
            }else{
                count -= 1;
            }
            if(count <= 0){
                return result.toString();
            }
        }
        return result.toString();
    }

    private BasicDBList parseMongoList(SpliceStringReader reader) {
        SpliceStringReader.Matcher matcher;
        BasicDBList result = new BasicDBList();
        while (!((matcher = reader.splice(mongoSymbols)).finish())) {
            switch (matcher.match()) {
                case "," : {
                    String value = matcher.prefix().trim();
                    if (value.length() > 0) {
                        result.add(parseMongoContent(value));
                    }
                    break;
                }
                case "{" :
                    result.add(parseMongoMap(reader));
                    break;
                case "[" :
                    result.add(parseMongoList(reader));
                    break;
                case "}" : {
                    String value = matcher.prefix().trim();
                    if (value.length() > 0) {
                        result.add(parseMongoContent(value));
                    }
                    return result;
                }
            }
        }
        return result;
    }

    private BasicDBObject parseMongoMap(SpliceStringReader reader) {
        SpliceStringReader.Matcher matcher;
        BasicDBObject result = new BasicDBObject();
        String currentKey = null;
        while (!((matcher = reader.splice(mongoSymbols)).finish())) {
            switch (matcher.match()) {
                case ":" :
                    currentKey = matcher.prefix().trim();
                    break;
                case "," : {
                    String value = matcher.prefix().trim();
                    if (currentKey != null && value.length() > 0) {
                        result.append(currentKey, parseMongoContent(value));
                        currentKey = null;
                    }
                    break;
                }
                case "{" :
                    if(currentKey != null){
                        result.append(currentKey, parseMongoMap(reader));
                        currentKey = null;
                    }
                    break;
                case "[" :
                    if(currentKey != null){
                        result.append(currentKey, parseMongoList(reader));
                        currentKey = null;
                    }
                    break;
                case "}" : {
                    String value = matcher.prefix().trim();
                    if (currentKey != null && value.length() > 0) {
                        result.append(currentKey, parseMongoContent(value));
                    }
                    return result;
                }
            }
        }
        return result;
    }

    private BasicDBObject parseActionInfo(SpliceStringReader reader) {
        SpliceStringReader.Matcher matcher;
        BasicDBObject result = new BasicDBObject();
        String currentKey = null;
        while(!((matcher = reader.splice(actionInfoSymbols)).finish())){
            switch(matcher.match()){
                case "=" :
                    currentKey = matcher.prefix().trim();
                    break;
                case "," : {
                    String value = matcher.prefix().trim();
                    if (currentKey != null && value.length() > 0) {
                        result.append(currentKey, value);
                        currentKey = null;
                    }
                    break;
                }
                case "[" :
                    if(currentKey != null){
                        result.append(currentKey, parseMongoList(reader));
                        currentKey = null;
                    }
                    break;
                case "{" :
                    if(currentKey != null){
                        result.append(currentKey, parseMongoMap(reader));
                        currentKey = null;
                    }
                    break;
                case ")" :
                case ">" :
                case "]" : {
                    String value = matcher.prefix().trim();
                    if (currentKey != null && value.length() > 0) {
                        result.append(currentKey, value);
                    }
                    return result;
                }
            }
        }
        return result;
    }

    private Action parseAction(SpliceStringReader reader) {
        SpliceStringReader.Matcher matcher = reader.splice(actionScopeSymbols);
        if(matcher.finish()) {
            return null;
        }

        String actionName = matcher.prefix().trim();
        Action.Scope scope = null;
        switch(matcher.match()){
            case "(" :
                scope = Action.Scope.PARENT;
                break;
            case "<" :
                scope = Action.Scope.SALF;
                break;
            case "[" :
                scope = Action.Scope.CHILD;
                break;
        }

        BasicDBObject infos = parseActionInfo(reader);

        switch(actionName){
            case "find" :
                return new Find(scope, (String)infos.get("db"), (String)infos.get("coll"), (BasicDBObject)infos.get("query"), (BasicDBObject)infos.get("projection"));
            case "findOne" :
                return new FindOne(scope, (String)infos.get("db"), (String)infos.get("coll"), (BasicDBObject)infos.get("query"), (BasicDBObject)infos.get("projection"));
            case "findOneById" :
                return new FindOneById(scope, (String)infos.get("db"), (String)infos.get("coll"), (BasicDBObject)infos.get("projection"));
            case "distinct" :
                return new Distinct(scope, (String)infos.get("db"), (String)infos.get("coll"), (String)infos.get("key"), (BasicDBObject)infos.get("query"));
            case "count" :
                return new Count(scope, (String)infos.get("db"), (String)infos.get("coll"), (BasicDBObject)infos.get("query"));
            case "aggregate" :
                return new Aggregate(scope, (String)infos.get("db"), (String)infos.get("coll"), (BasicDBList)infos.get("pipelines"));
        }

        return null;
    }

    private Dsl parseContent(SpliceStringReader reader) {
        SpliceStringReader.Matcher matcher = reader.splice(propertySymbols);
        if(matcher.finish()){
            return EmptyObject.Dsl;
        }

        Dsl dsl = new Dsl();
        do{
            switch(matcher.match()){
                case "@" :
                    parseActionProperty(dsl, reader);
                    break;
                case "{" :
                    String key = matcher.prefix().trim();
                    if(key.length() > 0){
                        Dsl childDsl = parseContent(matcher.reader());
                        if(!childDsl.isEmpty()){
                            dsl.appendDsl(key, childDsl);
                        }
                    }
                    break;
                case "}" :
                    return dsl;
            }
        }while(!((matcher = reader.splice(propertySymbols)).finish()));

        return dsl;
    }

    private void parseActionProperty(Dsl parent, SpliceStringReader reader) {
        List<Action> actions = new ArrayList<Action>();
        Action firstAction = parseAction(reader);
        if(firstAction != null){
            actions.add(firstAction);
        }
        SpliceStringReader.Matcher matcher;
        loop: while(!((matcher = reader.splice(propertySymbols)).finish())){
            String key = matcher.prefix().trim();
            switch(matcher.match()){
                case "@" :
                    if(key.length() > 0){
                        Dsl dsl = new Dsl();
                        dsl.appendAction(actions);
                        parent.appendDsl(key, dsl);
                    }else{
                        Action action = parseAction(reader);
                        if(action != null){
                            actions.add(action);
                        }
                    }
                    break;
                case "{" :
                    if(key.length() > 0){
                        Dsl childDsl = parseContent(matcher.reader());
                        childDsl.appendAction(actions);
                        if(!childDsl.isEmpty()){
                            parent.appendDsl(key, childDsl);
                        }
                    }
                    break;
                case "}" : {
                    if(key.length() > 0){
                        Dsl dsl = new Dsl();
                        dsl.appendAction(actions);
                        parent.appendDsl(key, dsl);
                    }
                    break loop;
                }
            }
        }
    }

}
