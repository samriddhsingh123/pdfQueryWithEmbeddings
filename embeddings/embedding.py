from flask import Flask, request, jsonify
from transformers import AutoTokenizer, AutoModel

app = Flask(__name__)

# Initialize the model inside a function
def load_model():
    model_name = "allenai/longformer-base-4096"
    tokenizer = AutoTokenizer.from_pretrained(model_name)
    model = AutoModel.from_pretrained(model_name)
    return tokenizer, model

# Use the 'if __name__ == '__main__':' block to avoid multiprocessing issues on Windows
if __name__ == '__main__':
    # Load the model and tokenizer
    tokenizer, model = load_model()

    @app.route('/v1/embeddings', methods=['POST'])
    def generate_embedding():
        data = request.get_json()
        text = data.get("text", "")
        inputs = tokenizer(text, return_tensors="pt")
        outputs = model(**inputs)
        embedding = outputs.last_hidden_state.mean(dim=1).detach().numpy().tolist()
        print(embedding)
        return jsonify({"embedding": embedding})

    # Start the Flask app
    app.run(host='0.0.0.0', port=11411)