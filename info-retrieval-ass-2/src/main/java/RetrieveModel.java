import org.apache.lucene.search.similarities.*;

import java.util.HashMap;
import java.util.Map;

public class RetrieveModel {
    private static final String BM25 = "BM25Similarity";
    private static final String BOOLEAN = "BooleanSimilarity";
    private static final String CLASSIC = "ClassicSimilarity";
    private static final String LM_DIRICHLET = "LMDirichletSimilarity";
    private static final String CLASSIC_LMDIRICHLET = "Classic_LMDirichletSimilarity";
    private static final String BM25_LMDIRICHLET = "BM25_LMDirichletSimilarity";

    public static final HashMap<String, Similarity> models = new HashMap<>();

    static {
        // hyperparameter in the model weight
        // k1: Parameter controlling saturation of lexical items, usually set between 1.2 and 2.0.
        // b: parameter controlling the length normalization, usually set at 0.5.
        //  similarity = new BM25Similarity();
        models.put(BM25, new BM25Similarity(1.5f,0.75f));
        models.put(BOOLEAN, new BooleanSimilarity());
        models.put(CLASSIC, new ClassicSimilarity());
        models.put(LM_DIRICHLET, new LMDirichletSimilarity());
        models.put(CLASSIC_LMDIRICHLET, new MultiSimilarity(new Similarity[]{new ClassicSimilarity(), new LMDirichletSimilarity()}));
        models.put(BM25_LMDIRICHLET, new MultiSimilarity(new Similarity[]{new BM25Similarity(), new LMDirichletSimilarity()}));
    }

    public static Similarity getModel(String model) {
        return models.get(model);
    }
}
