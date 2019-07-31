# Add faulty components to the PACCOR generated JSON componentsFile.
# This will be used to create a bad platform certificate.

import json

print("Adding Faulty components...")

try:
	nicComponent = '00090002'
	pc_dir = '/var/hirs/pc_generation/'

	with open(pc_dir + "componentsFile", "r") as f:

		data = json.load(f)
		print(data)
		components = data['COMPONENTS']
		for component in components:
			if component['COMPONENTCLASS']['COMPONENTCLASSVALUE'] == nicComponent:
				print("Creating FAULTY component for: " + component['MODEL'])
				component['MODEL'] += "-FAULTY"
				print("New JSON value: " + component['MODEL'])

	with open(pc_dir + "PBaseCertB.json", 'w') as outfile:
		json.dump(data, outfile)

except Exception as ex:
    print "=== ERROR generating PBaseCertB.json ===: error({0})".format(ex.message)
