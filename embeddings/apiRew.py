from flask import Flask, request, jsonify
from transformers import AutoTokenizer, AutoModelForCausalLM

app = Flask(__name__)

# Load the model locally
tokenizer = AutoTokenizer.from_pretrained("gpt2")
model = AutoModelForCausalLM.from_pretrained("gpt2")

@app.route("/generate", methods=["POST"])
def generate_text():
    data = request.json
    prompt = data.get("prompt", "")
    inputs = tokenizer(prompt, return_tensors="pt")

    # Adjust max_length or use max_new_tokens for better control
    outputs = model.generate(
        inputs["input_ids"], 
        max_new_tokens=20,  # Limits only the number of newly generated tokens
        num_return_sequences=1
    )
    generated_text = tokenizer.decode(outputs[0], skip_special_tokens=True)
    return jsonify({"response": generated_text})

if __name__ == "__main__":
    app.run(port=5000)
