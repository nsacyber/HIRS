# Create JSON files needed to create the following certificates:
#    SIDeltaCertB1 - Bad Delta
#	 VARDeltaCertB1 - Good Delta

import sys
import json
import copy
import pprint

try:
	pcDir = '/var/hirs/pc_generation/'
	pBaseJsonFileIn = 'PBaseCertB.componentlist.json'
	siDeltaB1JsonFileOut = 'SIDeltaCertB1.componentlist.json'
	varDeltaB1JsonFileOut = 'VARDeltaCertB1.componentlist.json'

	# Open the PBaseCertB components file
	with open(pcDir + pBaseJsonFileIn, "r") as f:

		# Load the info from the componentsFile
		data = json.load(f)
		print("The %s info:" % (pBaseJsonFileIn))
		pp = pprint.PrettyPrinter(indent=4)
		pp.pprint(data)

		# Initialize the structures
		siDeltaB1ComponentDict = copy.deepcopy(data)
		varDeltaB1ComponentDict = copy.deepcopy(data)

		# Remove all the components
		del siDeltaB1ComponentDict['COMPONENTS'][:]
		del varDeltaB1ComponentDict['COMPONENTS'][:]

		# Find "FAULTY" component from original data; and create the delta JSON files
		for component in data['COMPONENTS']:
			if component['MODEL'].__contains__("-FAULTY"):

				print("Found Faulty Component:")
				pp.pprint(component)

				# Make copy of component for SIDeltaCertB1
				siDeltaB1Component = copy.copy(component)

				# Change status to be "MODIFIED"
				print("Updated status to be MODIFIED...")
				siDeltaB1Component['STATUS'] = "MODIFIED"

				# Add to component SIDeltaCertB1 list
				print("Adding component to %s list..." % (siDeltaB1JsonFileOut))
				siDeltaB1ComponentDict['COMPONENTS'].append(siDeltaB1Component)

				# Make copy of component for VARDeltaCertB1
				varDeltaB1Component_1 = copy.copy(component)

				# Change status to be "REMOVED"
				print("Updated status to be REMOVED...")
				varDeltaB1Component_1['STATUS'] = "REMOVED"

				# Add to component VARDeltaCertB1 list
				print("Adding component to %s list..." % (varDeltaB1JsonFileOut))
				varDeltaB1ComponentDict['COMPONENTS'].append(varDeltaB1Component_1)

				# Make copy of component for VARDeltaCertB1
				varDeltaB1Component_2 = copy.copy(component)

				# Change status to be "ADDED"
				print("Updated status to be ADDED...")
				varDeltaB1Component_2['STATUS'] = "ADDED"

				# Remove "-FAULTY" substring in the model
				varDeltaB1Component_2['MODEL'] = varDeltaB1Component_2['MODEL'].replace('-FAULTY', '')
				print("Removed -FAULTY from component...")

				# Add to component VARDeltaCertB1 list
				print("Adding component to %s list..." % (varDeltaB1JsonFileOut))
				varDeltaB1ComponentDict['COMPONENTS'].append(varDeltaB1Component_2)
				break

	# Write the new JSON file to be used in creating the SIDeltaCertB1 certificate
	with open(pcDir + siDeltaB1JsonFileOut, 'w') as outfile:
		print("Writing %s%s ..." % (pcDir, siDeltaB1JsonFileOut))
 		json.dump(siDeltaB1ComponentDict, outfile)
 		pp = pprint.PrettyPrinter(indent=4)
		pp.pprint(siDeltaB1ComponentDict)

	# Write the new JSON file to be used in creating the VARDeltaCertB1 certificate
	with open(pcDir + varDeltaB1JsonFileOut, 'w') as outfile:
		print("Writing %s%s ..." % (pcDir, varDeltaB1JsonFileOut))
 		json.dump(varDeltaB1ComponentDict, outfile)
 		pp = pprint.PrettyPrinter(indent=4)
		pp.pprint(varDeltaB1ComponentDict)

except Exception as ex:
	print("=== ERROR generating PBaseCertB JSON files: %s" % (ex.message))
