# Add delta components to be used to create the Delta certificate.
import sys
import json
import copy
import pprint

print("Creating SIDeltaCertB1.componentlist.json...")

try:
	pc_dir = '/var/hirs/pc_generation/'

	# Open the JSON file from the PBasetCertB certificate.
	with open(pc_dir + "PBaseCertB.json", "r") as f:

		# Load the info from the PBaseCertB certificate.
		data = json.load(f)
		print("The PBaseCertB.json info:")
		pp = pprint.PrettyPrinter(indent=4)
		pp.pprint(data)

		# Get the components from the PBaseCertB certificate.
		components = data['COMPONENTS']

		# Initialize structures to work with.
		componentDict = {}
		componentDict["PLATFOM"] = data['PLATFORM']
		componentDict['COMPONENTS'] = []
		componentDict["PROPERTIES"] = data['PROPERTIES']
		updatedComponetList = []

		# Find "FAULTY" components to be removed; and change them to be good components.
		for component in components:
			if component['MODEL'].__contains__("-FAULTY"):

				print("Found Faulty Component:")
				pp.pprint(component)

				# Change status to be "REMOVED".
				print("Updated status to be REMOVED...")
				component['STATUS'] = "REMOVED"

				# Add to component list.
				print("Adding component to list...")
				updatedComponetList.append(component)

				# Make copy of above component.
				print("Created copy of component...")
				tmpComponent = copy.copy(component)

				# Change status to be "ADDED".
				print("Updated status to be ADDED...")
				tmpComponent['STATUS'] = "ADDED"

				# Remove "-FAULTY" substring in the model.
				tmpComponent['MODEL'] = tmpComponent['MODEL'].replace('-FAULTY', '')
				print("Removed -FAULTY from component model...")

				print("Adding this component to list: ")
				pp.pprint(tmpComponent)
				updatedComponetList.append(tmpComponent)

		# Update the component dictionary.
		componentDict['COMPONENTS'] = updatedComponetList
		print("The component list for Delta Certificate generation:")
		pp.pprint(componentDict)

	# Write the new JSON file to be used in creating the delta certificate.
	with open(pc_dir + "SIDeltaCertB1.componentlist.json", 'w') as outfile:
		print("Writing " + pc_dir + "SIDeltaCertB1.componentlist.json...")
 		json.dump(componentDict, outfile)

except Exception as ex:
	print "=== ERROR generating SIDeltaCertB1.componentlist.json ===: error({0})".format(ex.message)
