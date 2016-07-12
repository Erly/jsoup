package org.jsoup.select;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Base combining (and, or) evaluator.
 */
abstract class CombiningEvaluator extends Evaluator {
    final ArrayList<Evaluator> evaluators;
    int num = 0;

    CombiningEvaluator() {
        super();
        evaluators = new ArrayList<Evaluator>();
    }

    CombiningEvaluator(Collection<Evaluator> evaluators) {
        this();
        for (Evaluator evaluator : evaluators) {
            if (evaluator instanceof CombiningEvaluator.And) {
                this.evaluators.addAll(((CombiningEvaluator.And)evaluator).evaluators);
            } else {
                this.evaluators.add(evaluator);
            }
        }

        //prioritizeVisibleHiddenPseudoSelectors();

        updateNumEvaluators();
    }

    Evaluator rightMostEvaluator() {
        return num > 0 ? evaluators.get(num - 1) : null;
    }
    
    void replaceRightMostEvaluator(Evaluator replacement) {
        evaluators.set(num - 1, replacement);
    }

    void prioritizeVisibleHiddenPseudoSelectors() {
        for (int i = 0; i < evaluators.size(); i++) {
            if (evaluators.get(i) instanceof Evaluator.IsVisible || evaluators.get(i) instanceof Evaluator.IsHidden)
                evaluators.add(0, evaluators.remove(i));
        }
    }

    void updateNumEvaluators() {
        // used so we don't need to bash on size() for every match test
        num = evaluators.size();
    }

    static final class And extends CombiningEvaluator {
        And(Collection<Evaluator> evaluators) {
            super(evaluators);
        }

        And(Evaluator... evaluators) {
            this(Arrays.asList(evaluators));
        }

        @Override
        public boolean matches(Element root, Element node) {
            for (int i = 0; i < num; i++) {
                Evaluator s = evaluators.get(i);
                if (!s.matches(root, node))
                    return false;
            }
            return true;
        }

        @Override
        public boolean matches(Element root, Element node, int index, int collectionSize, int depth) {
            for (int i = 0; i < num; i++) {
                Evaluator s = evaluators.get(i);
                if (!s.matches(root, node, index, collectionSize, depth))
                    return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return StringUtil.join(evaluators, " ");
        }
    }

    static final class Or extends CombiningEvaluator {
        /**
         * Create a new Or evaluator. The initial evaluators are ANDed together and used as the first clause of the OR.
         * @param evaluators initial OR clause (these are wrapped into an AND evaluator).
         */
        Or(Collection<Evaluator> evaluators) {
            super();
            if (num > 1)
                this.evaluators.add(new And(evaluators));
            else // 0 or 1
                this.evaluators.addAll(evaluators);
            updateNumEvaluators();
        }

        Or() {
            super();
        }

        public void add(Evaluator e) {
            evaluators.add(e);
            updateNumEvaluators();
        }

        @Override
        public boolean matches(Element root, Element node) {
            for (int i = 0; i < num; i++) {
                Evaluator s = evaluators.get(i);
                if (s.matches(root, node))
                    return true;
            }
            return false;
        }

        @Override
        public boolean matches(Element root, Element node, int index, int collectionSize, int depth) {
            for (int i = 0; i < num; i++) {
                Evaluator s = evaluators.get(i);
                if (s.matches(root, node, index, collectionSize, depth))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format(":or%s", evaluators);
        }
    }
}
