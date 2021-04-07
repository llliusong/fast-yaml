package com.pine.fast.plugin.suggestion.completion;

import static java.util.Objects.requireNonNull;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.pine.fast.plugin.misc.GenericUtil;
import com.pine.fast.plugin.misc.PsiCustomUtil;
import com.pine.fast.plugin.suggestion.SuggestionNode;
import com.pine.fast.plugin.suggestion.service.SuggestionService;
import gnu.trove.THashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;
import org.jetbrains.yaml.psi.YAMLValue;

class YamlCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull final CompletionParameters completionParameters,
                                  final ProcessingContext processingContext, @NotNull final CompletionResultSet resultSet) {

        PsiElement element = completionParameters.getPosition();
        if (element instanceof PsiComment) {
            return;
        }

        Project project = element.getProject();
        Module module = PsiCustomUtil.findModule(element);

        SuggestionService service = ServiceManager.getService(project, SuggestionService.class);

        if ((module == null || !service.canProvideSuggestions(project, module))) {
            return;
        }

        // 已经存在的key，需要进行排除
        Set<String> siblingsToExclude = null;

        PsiElement elementContext = element.getContext();
        PsiElement parent = requireNonNull(elementContext).getParent();
        if (parent instanceof YAMLSequence) {
            // lets force user to create array element prefix before he can ask for suggestions
            return;
        }
        if (parent instanceof YAMLSequenceItem) {
            for (PsiElement child : parent.getParent().getChildren()) {
                if (child != parent) {
                    if (child instanceof YAMLSequenceItem) {
                        YAMLValue value = YAMLSequenceItem.class.cast(child).getValue();
                        if (value != null) {
                            siblingsToExclude = getNewIfNotPresent(siblingsToExclude);
                            siblingsToExclude.add(SuggestionNode.sanitise(value.getText()));
                        }
                    } else if (child instanceof YAMLKeyValue) {
                        siblingsToExclude = getNewIfNotPresent(siblingsToExclude);
                        siblingsToExclude.add(SuggestionNode.sanitise(YAMLKeyValue.class.cast(child).getKeyText()));
                    }
                }
            }
        } else if (parent instanceof YAMLMapping) {
            for (PsiElement child : parent.getChildren()) {
                if (child != elementContext) {
                    if (child instanceof YAMLKeyValue) {
                        siblingsToExclude = getNewIfNotPresent(siblingsToExclude);
                        siblingsToExclude.add(SuggestionNode.sanitise(YAMLKeyValue.class.cast(child).getKeyText()));
                    }
                }
            }
        }

        List<LookupElementBuilder> suggestions;
        // For top level element, since there is no parent parentKeyValue would be null
        String queryWithDotDelimitedPrefixes = GenericUtil.truncateIdeaDummyIdentifier(element);

        List<String> ancestralKeys = null;
        PsiElement context = elementContext;
        do {
            if (context instanceof YAMLKeyValue) {
                if (ancestralKeys == null) {
                    ancestralKeys = new ArrayList<>();
                }
                ancestralKeys.add(0, GenericUtil.truncateIdeaDummyIdentifier(((YAMLKeyValue) context).getKeyText()));
            }
            context = requireNonNull(context).getParent();
        } while (context != null);

        String s = "\\$\\{\\w+\\}=|\\$\\{\\w+\\} =";
        boolean contains = false;

        String handleStr = queryWithDotDelimitedPrefixes;
        String[] split = queryWithDotDelimitedPrefixes.split(s);
        if (split.length == 2) {
            contains = true;
            handleStr = split[1].trim();
        }

        suggestions = service
                .findSuggestionsForQueryPrefix(project, module, FileType.YAML, element, ancestralKeys,
                        queryWithDotDelimitedPrefixes, handleStr, siblingsToExclude);

        if (suggestions != null) {
            Consumer<LookupElementBuilder> addElement = contains ?  resultSet.withPrefixMatcher(handleStr)::addElement :resultSet::addElement;
            suggestions.forEach(addElement);
        }
    }

    @NotNull
    private Set<String> getNewIfNotPresent(@Nullable Set<String> siblingsToExclude) {
        if (siblingsToExclude == null) {
            return new THashSet<>();
        }
        return siblingsToExclude;
    }

}
