from transformers import AutoTokenizer, AutoModelForCausalLM

# Specify the model you want
model_name = "gpt2"  # Replace with another free model if desired

# Download tokenizer and model locally
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(model_name)
