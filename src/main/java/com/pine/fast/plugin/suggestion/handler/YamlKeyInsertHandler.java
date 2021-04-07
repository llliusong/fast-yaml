package com.pine.fast.plugin.suggestion.handler;

import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;
import static com.intellij.openapi.editor.EditorModificationUtil.insertStringAtCaret;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.pine.fast.plugin.misc.GenericUtil;
import com.pine.fast.plugin.misc.PsiCustomUtil;
import com.pine.fast.plugin.suggestion.OriginalNameProvider;
import com.pine.fast.plugin.suggestion.Suggestion;
import com.pine.fast.plugin.suggestion.SuggestionNodeType;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class YamlKeyInsertHandler implements InsertHandler<LookupElement> {

    @Override
    public void handleInsert(final InsertionContext context, final LookupElement lookupElement) {
        if (!nextCharAfterSpacesAndQuotesIsColon(getStringAfterAutoCompletedValue(context))) {
            String existingIndentation = getExistingIndentation(context, lookupElement);
            Suggestion suggestion = (Suggestion) lookupElement.getObject();
            String indentPerLevel = GenericUtil.getCodeStyleIntent(context);
            String suggestionWithCaret =
                    getSuggestionReplacementWithCaret(suggestion, existingIndentation,
                            indentPerLevel);
            String suggestionWithoutCaret = suggestionWithCaret.replace(SuggestionNodeType.CARET, "");

            PsiElement currentElement = context.getFile().findElementAt(context.getStartOffset());
            assert currentElement != null : "no element at " + context.getStartOffset();

            this.deleteLookupTextAndRetrieveOldValue(context, currentElement);

            insertStringAtCaret(context.getEditor(), suggestionWithoutCaret, false, true,
                    getCaretIndex(suggestionWithCaret));
        }
    }

    private int getCaretIndex(final String suggestionWithCaret) {
        return suggestionWithCaret.indexOf(SuggestionNodeType.CARET);
    }

    private String getExistingIndentation(final InsertionContext context, final LookupElement item) {
        final String stringBeforeAutoCompletedValue = getStringBeforeAutoCompletedValue(context, item);
        return getExistingIndentationInRowStartingFromEnd(stringBeforeAutoCompletedValue);
    }

    @NotNull
    private String getStringAfterAutoCompletedValue(final InsertionContext context) {
        return context.getDocument().getText().substring(context.getTailOffset());
    }

    @NotNull
    private String getStringBeforeAutoCompletedValue(final InsertionContext context,
                                                     final LookupElement item) {
        return context.getDocument().getText()
                .substring(0, context.getTailOffset() - item.getLookupString().length());
    }

    private boolean nextCharAfterSpacesAndQuotesIsColon(final String string) {
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (c != ' ' && c != '"') {
                return c == ':';
            }
        }
        return false;
    }

    private String getExistingIndentationInRowStartingFromEnd(final String val) {
        int count = 0;
        for (int i = val.length() - 1; i >= 0; i--) {
            final char c = val.charAt(i);
            if (c != '\t' && c != ' ' && c != '-') {
                break;
            }
            count++;
        }
        return val.substring(val.length() - count, val.length()).replaceAll("-", " ");
    }

    private void deleteLookupTextAndRetrieveOldValue(InsertionContext context,
                                                     @NotNull PsiElement elementAtCaret) {
        if (elementAtCaret.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
            deleteLookupPlain(context);
        } else {
            YAMLKeyValue keyValue = PsiTreeUtil.getParentOfType(elementAtCaret, YAMLKeyValue.class);
            assert keyValue != null;
            context.commitDocument();

            // TODO: Whats going on here?
            if (keyValue.getValue() != null) {
                YAMLKeyValue dummyKV =
                        YAMLElementGenerator.getInstance(context.getProject()).createYamlKeyValue("foo", "b");
                dummyKV.setValue(keyValue.getValue());
            }

            context.setTailOffset(keyValue.getTextRange().getEndOffset());
            runWriteCommandAction(context.getProject(),
                    () -> keyValue.getParentMapping().deleteKeyValue(keyValue));
        }
    }

    private void deleteLookupPlain(InsertionContext context) {
        Document document = context.getDocument();
        document.deleteString(context.getStartOffset(), context.getTailOffset());
        context.commitDocument();
    }

    @NotNull
    private String getSuggestionReplacementWithCaret(Suggestion suggestion,
                                                     String existingIndentation, String indentPerLevel) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        List<? extends OriginalNameProvider> matchesTopFirst = suggestion.getMatchesForReplacement();
        do {
            OriginalNameProvider nameProvider = matchesTopFirst.get(i);

            builder.append("\n").append(existingIndentation).append(GenericUtil.getIndent(indentPerLevel, i))
                    .append(nameProvider.getOriginalName());
            // 根据配置，获取是否在敲击回车后追加：
            if (suggestion.getIsAppendColon()) {
                builder.append(":");
            }
            i++;
        } while (i < matchesTopFirst.size());
        builder.delete(0, existingIndentation.length() + 1);
        String indentForNextLevel =
                GenericUtil.getOverallIndent(existingIndentation, indentPerLevel, matchesTopFirst.size());
        String sufix = getPlaceholderSufixWithCaret(suggestion, indentForNextLevel);
        builder.append(sufix);
        return builder.toString();
    }

    @NotNull
    private String getPlaceholderSufixWithCaret(Suggestion suggestion,
                                                String indentForNextLevel) {
        if (suggestion.getLastSuggestionNode().isMetadataNonProperty()) {
            return "\n" + indentForNextLevel + SuggestionNodeType.CARET;
        }
        return SuggestionNodeType.CARET;
    }

}
