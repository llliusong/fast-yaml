package com.pine.fast.plugin.misc;

import static com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED;
import static com.intellij.codeInsight.documentation.DocumentationManager.createHyperlink;
import static com.intellij.openapi.util.text.StringUtil.containsChar;
import static com.intellij.openapi.util.text.StringUtil.endsWithChar;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static com.intellij.openapi.util.text.StringUtil.replace;
import static java.text.BreakIterator.getSentenceInstance;
import static java.util.Arrays.asList;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * 通用工具类
 *
 * @author pine
 */
@UtilityClass
public class GenericUtil {

    private static final Pattern PACKAGE_REMOVAL_PATTERN =
            Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*\\.");
    private static final Pattern GENERIC_SECTION_REMOVAL_PATTERN =
            Pattern.compile("<(?<commaDelimitedTypes>[^>]+)>");
    //  private static final Pattern CLASSNAME_MATCH_PATTERN =
    //      Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*(?:\\.[a-zA-Z_][a-zA-Z_0-9]*)*)");

    private static Pattern methodToFragmentConverter = Pattern.compile("(.+)\\.(.+)\\(.*\\)");

    public static String typeForDocumentationNavigation(String type) {
        return type.replaceAll("\\$", ".");
    }

    public static void updateClassNameAsJavadocHtml(StringBuilder buffer, String type) {
        Matcher matcher = GENERIC_SECTION_REMOVAL_PATTERN.matcher(type);
        String baseClass = type;
        boolean parametersPresent = matcher.find();
        String[] typeParameters = null;
        if (parametersPresent) {
            typeParameters = matcher.group("commaDelimitedTypes").split(",");
            baseClass = matcher.replaceAll("");
        }
        createHyperlink(buffer, typeForDocumentationNavigation(baseClass), baseClass, false);
        if (typeParameters != null) {
            buffer.append("&lt;");
            for (int i = 0; i < typeParameters.length; i++) {
                updateClassNameAsJavadocHtml(buffer, typeParameters[i]);
                if (i != typeParameters.length - 1) {
                    buffer.append(", ");
                }
            }
            buffer.append("&gt;");
        }
    }

    public static String methodForDocumentationNavigation(String typeAndMethod) {
        return methodToFragmentConverter.matcher(typeForDocumentationNavigation(typeAndMethod))
                .replaceAll("$1#$2");
    }

    @NotNull
    public static String getCodeStyleIntent(InsertionContext insertionContext) {
        final CodeStyleSettings currentSettings = CodeStyle.getSettings(insertionContext.getProject());
        final CommonCodeStyleSettings.IndentOptions indentOptions =
                currentSettings.getIndentOptions(insertionContext.getFile().getFileType());
        return indentOptions.USE_TAB_CHARACTER ?
                "\t" :
                StringUtil.repeatSymbol(' ', indentOptions.INDENT_SIZE);
    }

    @NotNull
    public static String getFirstSentenceWithoutDot(String fullSentence) {
        if (containsChar(fullSentence, '.')) {
            BreakIterator breakIterator = getSentenceInstance(Locale.US);
            breakIterator.setText(fullSentence);
            fullSentence = fullSentence.substring(breakIterator.first(), breakIterator.next()).trim();
        }

        if (isNotEmpty(fullSentence)) {
            String withoutDot = endsWithChar(fullSentence, '.') ?
                    fullSentence.substring(0, fullSentence.length() - 1) :
                    fullSentence;
            return replace(withoutDot, "\n", "");
        } else {
            return "";
        }
    }

    public static String truncateIdeaDummyIdentifier(@NotNull PsiElement element) {
        return truncateIdeaDummyIdentifier(element.getText());
    }

    public static String truncateIdeaDummyIdentifier(String text) {
        return text.replace(DUMMY_IDENTIFIER_TRIMMED, "");
    }

    @SafeVarargs
    public static <T> List<T> modifiableList(T... items) {
        return new ArrayList<>(asList(items));
    }

    public static <T> List<T> newListWithMembers(List<T> itemsToCopy, T newItem) {
        ArrayList<T> newModifiableList = new ArrayList<>(itemsToCopy);
        newModifiableList.add(newItem);
        return newModifiableList;
    }

    public static String removeGenerics(String type) {
        Matcher matcher = GENERIC_SECTION_REMOVAL_PATTERN.matcher(type);
        if (matcher.find()) {
            return matcher.replaceAll("");
        }
        return type;
    }

    public static String shortenedType(String type) {
        if (type == null) {
            return null;
        }
        Matcher matcher = PACKAGE_REMOVAL_PATTERN.matcher(type);
        if (matcher.find()) {
            return matcher.replaceAll("");
        }
        return type;
    }


    public static String dotDelimitedOriginalNames(List<? extends SuggestionNode> matches) {
        return dotDelimitedOriginalNames(matches, 0);
    }

    public static String dotDelimitedNames(List<? extends SuggestionNode> matches,
                                                   int startIndex) {
        StringBuilder builder = new StringBuilder();

        for (int i = startIndex; i < matches.size(); i++) {
            String name = matches.get(i).getName();
            if (name != null) {
                builder.append(name);
                boolean appendDot = i < matches.size() - 1;
                if (appendDot) {
                    builder.append(".");
                }
            }
        }
        return builder.toString();
    }


    public static String dotDelimitedOriginalNames(List<? extends SuggestionNode> matches,
                                                   int startIndex) {
        StringBuilder builder = new StringBuilder();

        for (int i = startIndex; i < matches.size(); i++) {
            String originalName = matches.get(i).getOriginalName();
            if (originalName != null) {
                builder.append(originalName);
                boolean appendDot = i < matches.size() - 1;
                if (appendDot) {
                    builder.append(".");
                }
            }
        }
        return builder.toString();
    }

    @NotNull
    public static String getIndent(String indent, int numOfHops) {
        if (numOfHops == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numOfHops; i++) {
            builder.append(indent);
        }
        return builder.toString();
    }

    @NotNull
    public static String getOverallIndent(String existingIndentation, String indentPerLevel,
                                          int numOfLevels) {
        return existingIndentation + getIndent(indentPerLevel, numOfLevels);
    }

    @NotNull
    public static <T extends Comparable<T>> SortedSet<T> newSingleElementSortedSet(T t) {
        SortedSet<T> suggestions = new TreeSet<>();
        suggestions.add(t);
        return suggestions;
    }

}
