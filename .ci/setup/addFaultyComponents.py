# Add faulty components to the PACCOR generated JSON componentsFile.
# This will be used to create a bad platform certificate.

import json

print("Adding Faulty components...")

try:
	with open("/var/hirs/pc_generation/componentsFile", "r") as f:
		data = json.load(f)
		#print(data)
		components = data["COMPONENTS"]
		for component in components:
			#print(component)
			for key, value in component.items():
				if key == "MODEL":
					print (key, value)
					if "ethernet" in value.lower():
						component["MODEL"] = value + "-FAULTY"
	#print(data)
	with open("/var/hirs/pc_generation/badComponentsFile", 'w') as outfile:
		json.dump(data, outfile)

except ValueError:
    print("Error processing JSON")
